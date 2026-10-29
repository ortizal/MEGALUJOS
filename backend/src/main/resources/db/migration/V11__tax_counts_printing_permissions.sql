-- ============================================================
-- V11 - Impuestos configurables, conteos, caja por medio de pago,
--       cola de impresión y permisos adicionales
-- ============================================================

-- 1. Reglas tributarias configurables (nunca tasas fijas en código)
CREATE TABLE tax_rules (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code varchar(30) NOT NULL UNIQUE,
    name varchar(100) NOT NULL,
    rate numeric(9,5) NOT NULL CHECK(rate >= 0 AND rate <= 1),
    is_default boolean NOT NULL DEFAULT false,
    valid_from date NOT NULL DEFAULT CURRENT_DATE,
    valid_to date,
    active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    CHECK(valid_to IS NULL OR valid_to >= valid_from)
);

CREATE UNIQUE INDEX ux_tax_rules_single_default ON tax_rules(is_default) WHERE is_default;

INSERT INTO tax_rules (code, name, rate, is_default, valid_from) VALUES
    ('IVA_15', 'IVA 15%', 0.15, true, DATE '2024-01-01'),
    ('IVA_0', 'IVA 0%', 0.00, false, DATE '2024-01-01');

-- 2. Snapshot de impuesto aplicado a documentos/ventas (referencia histórica)
CREATE TABLE tax_snapshots (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    source_type varchar(40) NOT NULL,
    source_id uuid NOT NULL,
    tax_code varchar(30) NOT NULL,
    tax_rate numeric(9,5) NOT NULL,
    taxable_amount numeric(18,2) NOT NULL DEFAULT 0,
    tax_amount numeric(18,2) NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_tax_snapshots_source ON tax_snapshots(source_type, source_id);

-- 3. Conteos físicos de inventario
CREATE TABLE stock_counts (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    count_number varchar(40) NOT NULL UNIQUE,
    warehouse_id uuid NOT NULL REFERENCES warehouses(id),
    status varchar(30) NOT NULL CHECK(status IN ('OPEN', 'COUNTED', 'APPROVED', 'CANCELLED')),
    notes text,
    created_by uuid NOT NULL REFERENCES users(id),
    approved_by uuid REFERENCES users(id),
    created_at timestamptz NOT NULL DEFAULT now(),
    completed_at timestamptz,
    approved_at timestamptz
);

CREATE TABLE stock_count_items (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    count_id uuid NOT NULL REFERENCES stock_counts(id) ON DELETE CASCADE,
    variant_id uuid NOT NULL REFERENCES product_variants(id),
    system_quantity numeric(18,3) NOT NULL DEFAULT 0,
    counted_quantity numeric(18,3) NOT NULL CHECK(counted_quantity >= 0),
    difference numeric(18,3) NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX ux_stock_count_item ON stock_count_items(count_id, variant_id);

-- 4. Arqueo por medio de pago en cierre de caja
CREATE TABLE cash_counts (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id uuid NOT NULL REFERENCES cash_sessions(id) ON DELETE CASCADE,
    method varchar(30) NOT NULL,
    expected_amount numeric(18,2) NOT NULL DEFAULT 0,
    counted_amount numeric(18,2) NOT NULL DEFAULT 0,
    difference numeric(18,2) NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_cash_count_method ON cash_counts(session_id, method);

-- 5. Cola de trabajos de impresión (ticket 80mm, A4, etiquetas)
CREATE TABLE print_jobs (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    job_type varchar(40) NOT NULL CHECK(job_type IN ('SALE_TICKET', 'CASH_OPENING', 'CASH_CLOSING', 'SERVICE_ORDER', 'PRODUCT_LABEL', 'RIDE')),
    source_type varchar(50) NOT NULL,
    source_id uuid NOT NULL,
    format varchar(20) NOT NULL DEFAULT 'THERMAL_80' CHECK(format IN ('THERMAL_80', 'THERMAL_58', 'A4')),
    status varchar(30) NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING', 'PRINTED', 'FAILED', 'CANCELLED')),
    copies integer NOT NULL DEFAULT 1 CHECK(copies > 0),
    printer_name varchar(120),
    payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    error text,
    requested_by uuid REFERENCES users(id),
    created_at timestamptz NOT NULL DEFAULT now(),
    printed_at timestamptz
);
CREATE INDEX ix_print_jobs_status ON print_jobs(status, created_at);

-- 6. Permisos adicionales
INSERT INTO permissions (id, code, name, module) VALUES
    ('00000000-0000-0000-0006-000000000010', 'sales:read', 'Consultar ventas e historial', 'POS'),
    ('00000000-0000-0000-0006-000000000011', 'sales:return', 'Registrar devoluciones', 'POS'),
    ('00000000-0000-0000-0006-000000000012', 'inventory:count', 'Conteos físicos de inventario', 'INVENTORY'),
    ('00000000-0000-0000-0006-000000000013', 'customers:manage', 'Gestionar clientes y vehículos', 'CUSTOMERS'),
    ('00000000-0000-0000-0006-000000000014', 'purchasing:manage', 'Gestionar compras y proveedores', 'PURCHASING'),
    ('00000000-0000-0000-0006-000000000015', 'users:manage', 'Gestionar usuarios y roles', 'IDENTITY'),
    ('00000000-0000-0000-0006-000000000016', 'showcase:manage', 'Gestionar vitrina y promociones', 'SHOWCASE'),
    ('00000000-0000-0000-0006-000000000017', 'settings:manage', 'Configuración del negocio', 'SETTINGS'),
    ('00000000-0000-0000-0006-000000000018', 'invoicing:read', 'Consultar comprobantes electrónicos', 'INVOICING'),
    ('00000000-0000-0000-0006-000000000019', 'services:read', 'Consultar órdenes de servicio', 'SERVICES'),
    ('00000000-0000-0000-0006-000000000020', 'print:manage', 'Gestionar trabajos de impresión', 'PRINT'),
    ('00000000-0000-0000-0006-000000000021', 'audit:read', 'Consultar auditoría', 'IDENTITY')
ON CONFLICT (code) DO NOTHING;

-- Administrador: todos los permisos
INSERT INTO role_permissions (role_id, permission_id)
SELECT '00000000-0000-0000-0005-000000000001', id FROM permissions
ON CONFLICT DO NOTHING;

-- Supervisor: todo excepto gestión de usuarios y configuración sensible
INSERT INTO role_permissions (role_id, permission_id)
SELECT '00000000-0000-0000-0005-000000000002', id FROM permissions
WHERE code NOT IN ('users:manage', 'settings:manage')
ON CONFLICT DO NOTHING;

-- Cajero: ventas, clientes, consulta de servicios y facturación
INSERT INTO role_permissions (role_id, permission_id)
SELECT '00000000-0000-0000-0005-000000000003', id FROM permissions
WHERE code IN ('sales:read', 'customers:manage', 'services:read', 'invoicing:read', 'invoicing:issue', 'print:manage')
ON CONFLICT DO NOTHING;

-- Bodeguero: inventario, compros y catálogo de lectura
INSERT INTO role_permissions (role_id, permission_id)
SELECT '00000000-0000-0000-0005-000000000004', id FROM permissions
WHERE code IN ('inventory:read', 'inventory:adjust', 'inventory:count', 'purchasing:manage', 'catalog:read')
ON CONFLICT DO NOTHING;

-- Técnico: servicios
INSERT INTO role_permissions (role_id, permission_id)
SELECT '00000000-0000-0000-0005-000000000005', id FROM permissions
WHERE code IN ('services:manage', 'services:read', 'catalog:read')
ON CONFLICT DO NOTHING;
