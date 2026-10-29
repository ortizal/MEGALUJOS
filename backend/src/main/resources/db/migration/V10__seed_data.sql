-- ============================================================
-- V10__seed_data.sql - DATOS INICIALES PARA MEGALUJOS
-- ============================================================

-- 1. Sucursal Matriz
INSERT INTO branches (id, code, name, address, phone, active)
VALUES ('00000000-0000-0000-0001-000000000001', 'SUC-001', 'MEGALUJOS Matriz', 'Av. Principal y Automotriz #102, Quito, Ecuador', '0999999999', true)
ON CONFLICT (code) DO NOTHING;

-- 2. Bodega Principal
INSERT INTO warehouses (id, branch_id, code, name, is_main, active)
VALUES ('00000000-0000-0000-0002-000000000001', '00000000-0000-0000-0001-000000000001', 'BOD-01', 'Bodega Principal Matriz', true, true)
ON CONFLICT (branch_id, code) DO NOTHING;

-- 3. Caja Registradora Principal
INSERT INTO cash_registers (id, branch_id, code, name, active)
VALUES ('00000000-0000-0000-0003-000000000001', '00000000-0000-0000-0001-000000000001', 'CAJA-01', 'Caja Mostrador 1', true)
ON CONFLICT (branch_id, code) DO NOTHING;

-- 4. Emisor SRI y Secuencias
INSERT INTO issuers (id, branch_id, tax_id, legal_name, trade_name, main_address, establishment_code, emission_point_code, environment, active)
VALUES ('00000000-0000-0000-0004-000000000001', '00000000-0000-0000-0001-000000000001', '1790012345001', 'MEGALUJOS ACCESORIOS AUTOMOTRICES CIA. LTDA.', 'MEGALUJOS', 'Av. Principal y Automotriz #102', '001', '001', 'TEST', true)
ON CONFLICT DO NOTHING;

INSERT INTO document_sequences (id, issuer_id, document_type, next_number)
VALUES 
    ('00000000-0000-0000-0004-000000000011', '00000000-0000-0000-0004-000000000001', '01', 1), -- Factura
    ('00000000-0000-0000-0004-000000000012', '00000000-0000-0000-0004-000000000001', '04', 1)  -- Nota de Crédito
ON CONFLICT DO NOTHING;

-- 5. Roles Base
INSERT INTO roles (id, code, name, description) VALUES
    ('00000000-0000-0000-0005-000000000001', 'ROLE_ADMIN', 'Administrador', 'Control total del sistema y configuración'),
    ('00000000-0000-0000-0005-000000000002', 'ROLE_SUPERVISOR', 'Supervisor', 'Supervisión de caja, autorizaciones y reportes'),
    ('00000000-0000-0000-0005-000000000003', 'ROLE_CASHIER', 'Cajero', 'Operación de POS, ventas y cobros en caja asignada'),
    ('00000000-0000-0000-0005-000000000004', 'ROLE_WAREHOUSE', 'Bodeguero', 'Recepción de compras y movimientos de inventario'),
    ('00000000-0000-0000-0005-000000000005', 'ROLE_TECHNICIAN', 'Técnico', 'Ejecución de stickers, impresiones e instalaciones')
ON CONFLICT (code) DO NOTHING;

-- 6. Permisos
INSERT INTO permissions (id, code, name, module) VALUES
    ('00000000-0000-0000-0006-000000000001', 'catalog:read', 'Ver catálogo', 'CATALOG'),
    ('00000000-0000-0000-0006-000000000002', 'catalog:write', 'Crear y editar catálogo', 'CATALOG'),
    ('00000000-0000-0000-0006-000000000003', 'inventory:read', 'Consultar inventario', 'INVENTORY'),
    ('00000000-0000-0000-0006-000000000004', 'inventory:adjust', 'Ajustar inventario', 'INVENTORY'),
    ('00000000-0000-0000-0006-000000000005', 'sales:create', 'Registrar ventas en POS', 'POS'),
    ('00000000-0000-0000-0006-000000000006', 'cash:manage', 'Apertura y cierre de caja', 'CASH'),
    ('00000000-0000-0000-0006-000000000007', 'services:manage', 'Gestionar órdenes de stickers e instalación', 'SERVICES'),
    ('00000000-0000-0000-0006-000000000008', 'invoicing:issue', 'Emitir facturación electrónica SRI', 'INVOICING'),
    ('00000000-0000-0000-0006-000000000009', 'reports:view', 'Visualizar reportes gerenciales', 'REPORTS')
ON CONFLICT (code) DO NOTHING;

-- Asignar permisos al Administrador
INSERT INTO role_permissions (role_id, permission_id)
SELECT '00000000-0000-0000-0005-000000000001', id FROM permissions
ON CONFLICT DO NOTHING;

-- Asignar permisos a Cajero
INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('00000000-0000-0000-0005-000000000003', '00000000-0000-0000-0006-000000000001'),
    ('00000000-0000-0000-0005-000000000003', '00000000-0000-0000-0006-000000000005'),
    ('00000000-0000-0000-0005-000000000003', '00000000-0000-0000-0006-000000000006')
ON CONFLICT DO NOTHING;

-- 7. Usuario Administrador ('admin' / contraseña: 'admin123')
INSERT INTO users (id, username, password_hash, full_name, email, active)
VALUES (
    '00000000-0000-0000-0007-000000000001',
    'admin',
    crypt('admin123', gen_salt('bf', 10)),
    'Administrador Megalujos',
    'admin@megalujos.com.ec',
    true
)
ON CONFLICT (username) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
VALUES ('00000000-0000-0000-0007-000000000001', '00000000-0000-0000-0005-000000000001')
ON CONFLICT DO NOTHING;

-- Usuario Cajero ('cajero1' / contraseña: 'cajero123')
INSERT INTO users (id, username, password_hash, full_name, email, active)
VALUES (
    '00000000-0000-0000-0007-000000000002',
    'cajero1',
    crypt('cajero123', gen_salt('bf', 10)),
    'Carlos Cajero Mostrador',
    'cajero1@megalujos.com.ec',
    true
)
ON CONFLICT (username) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
VALUES ('00000000-0000-0000-0007-000000000002', '00000000-0000-0000-0005-000000000003')
ON CONFLICT DO NOTHING;

-- 8. Cliente Genérico Consumidor Final
INSERT INTO customers (id, document_type, document_number, name, email, phone, address)
VALUES (
    '00000000-0000-0000-0008-000000000001',
    'CONSUMIDOR_FINAL',
    '9999999999999',
    'CONSUMIDOR FINAL',
    'consumidor@megalujos.ec',
    '0999999999',
    'Local Matriz'
)
ON CONFLICT DO NOTHING;

-- 9. Categorías Automotrices
INSERT INTO categories (id, name, slug, description, sort_order, active) VALUES
    ('00000000-0000-0000-0009-000000000001', 'Luces LED y Faros', 'luces-led-y-faros', 'Bombillos LED turbo, barras de luz, ojos de ángel y neblineros de alta intensidad', 1, true),
    ('00000000-0000-0000-0009-000000000002', 'Audio y Sonido', 'audio-y-sonido', 'Radios con pantalla Android, parlantes, bajos amplificados y tweeters', 2, true),
    ('00000000-0000-0000-0009-000000000003', 'Aceites y Lubricantes', 'aceites-y-lubricantes', 'Aceites sintéticos y semisintéticos de alto rendimiento para motor y transmisión', 3, true),
    ('00000000-0000-0000-0009-000000000004', 'Accesorios y Tuning', 'accesorios-y-tuning', 'Volantes deportivos, cubre asientos premium, perillas y embellecedores', 4, true),
    ('00000000-0000-0000-0009-000000000005', 'Stickers y Personalización', 'stickers-y-personalizacion', 'Diseño e impresión de vinilos automotrices, parasoles y franjas tuning', 5, true),
    ('00000000-0000-0000-0009-000000000006', 'Servicios de Taller', 'servicios-de-taller', 'Instalaciones eléctricas, calibración de audio e instalación de accesorios', 6, true)
ON CONFLICT (slug) DO NOTHING;

-- 10. Marcas
INSERT INTO brands (id, name, active) VALUES
    ('00000000-0000-0000-000a-000000000001', 'Osram', true),
    ('00000000-0000-0000-000a-000000000002', 'Pioneer', true),
    ('00000000-0000-0000-000a-000000000003', 'Motul', true),
    ('00000000-0000-0000-000a-000000000004', 'Sparco', true),
    ('00000000-0000-0000-000a-000000000005', 'Megalujos Custom', true)
ON CONFLICT (name) DO NOTHING;

-- 11. Productos y Variantes Iniciales
-- Producto 1: Luces LED H4 Turbo
INSERT INTO products (id, category_id, brand_id, sku, name, description, product_type, unit, track_stock, published, active)
VALUES (
    '00000000-0000-0000-000b-000000000001',
    '00000000-0000-0000-0009-000000000001',
    '00000000-0000-0000-000a-000000000001',
    'MEGA-LED-H4-120W',
    'Focos LED H4 Turbo Canbus 120W 20000LM',
    'Kit de bombillos LED automotrices H4 con enfriamiento activo por turboventilador. Luz blanco frío 6000K, disipador de aluminio aeronáutico y compatibilidad universal Canbus.',
    'PHYSICAL', 'SET', true, true, true
) ON CONFLICT (sku) DO NOTHING;

INSERT INTO product_variants (id, product_id, sku, barcode, attributes, sale_price, cost_price, min_stock, active)
VALUES (
    '00000000-0000-0000-000c-000000000001',
    '00000000-0000-0000-000b-000000000001',
    'MEGA-LED-H4-6000K',
    '7860000000011',
    '{"temperature": "6000K", "connector": "H4", "lumens": "20000LM"}',
    35.00, 18.50, 5, true
) ON CONFLICT (sku) DO NOTHING;

-- Producto 2: Radio Pantalla Android CarPlay
INSERT INTO products (id, category_id, brand_id, sku, name, description, product_type, unit, track_stock, published, active)
VALUES (
    '00000000-0000-0000-000b-000000000002',
    '00000000-0000-0000-0009-000000000002',
    '00000000-0000-0000-000a-000000000002',
    'MEGA-RAD-AND-9INCH',
    'Radio Pantalla Táctil Android 9" CarPlay & Android Auto',
    'Pantalla IPS HD de 9 pulgadas, procesador Octa-Core, 2GB RAM + 32GB ROM, soporte inalámbrico Apple CarPlay y Android Auto, ecualizador DSP 32 bandas.',
    'PHYSICAL', 'UNIT', true, true, true
) ON CONFLICT (sku) DO NOTHING;

INSERT INTO product_variants (id, product_id, sku, barcode, attributes, sale_price, cost_price, min_stock, active)
VALUES (
    '00000000-0000-0000-000c-000000000002',
    '00000000-0000-0000-000b-000000000002',
    'MEGA-RAD-AND-9INCH-2-32',
    '7860000000028',
    '{"screen": "9 inch IPS", "ram": "2GB", "storage": "32GB"}',
    145.00, 85.00, 3, true
) ON CONFLICT (sku) DO NOTHING;

-- Producto 3: Aceite Sintético Motul 5W40
INSERT INTO products (id, category_id, brand_id, sku, name, description, product_type, unit, track_stock, published, active)
VALUES (
    '00000000-0000-0000-000b-000000000003',
    '00000000-0000-0000-0009-000000000003',
    '00000000-0000-0000-000a-000000000003',
    'MEGA-OIL-MOTUL-5W40',
    'Aceite Sintético Motul 8100 X-cess 5W40 5 Litros',
    'Lubricante 100% sintético de altas prestaciones para motores a gasolina y diésel modernos de inyección directa y turbo.',
    'PHYSICAL', 'GAL', true, true, true
) ON CONFLICT (sku) DO NOTHING;

INSERT INTO product_variants (id, product_id, sku, barcode, attributes, sale_price, cost_price, min_stock, active)
VALUES (
    '00000000-0000-0000-000c-000000000003',
    '00000000-0000-0000-000b-000000000003',
    'MEGA-OIL-MOTUL-5W40-5L',
    '7860000000035',
    '{"viscosity": "5W40", "volume": "5L", "type": "100% Sintetico"}',
    48.50, 32.00, 4, true
) ON CONFLICT (sku) DO NOTHING;

-- Producto 4: Parlantes Pioneer 6x9
INSERT INTO products (id, category_id, brand_id, sku, name, description, product_type, unit, track_stock, published, active)
VALUES (
    '00000000-0000-0000-000b-000000000004',
    '00000000-0000-0000-0009-000000000002',
    '00000000-0000-0000-000a-000000000002',
    'MEGA-SPK-PIO-6X9',
    'Parlantes Coaxiales Pioneer 6x9" 4 Vías 450W',
    'Par de parlantes ovalados de alta fidelidad con cono de polipropileno moldeado por inyección y tweeter de cúpula balanceada.',
    'PHYSICAL', 'PAIR', true, true, true
) ON CONFLICT (sku) DO NOTHING;

INSERT INTO product_variants (id, product_id, sku, barcode, attributes, sale_price, cost_price, min_stock, active)
VALUES (
    '00000000-0000-0000-000c-000000000004',
    '00000000-0000-0000-000b-000000000004',
    'MEGA-SPK-PIO-6X9-450W',
    '7860000000042',
    '{"size": "6x9 inch", "power_max": "450W", "ways": "4"}',
    65.00, 38.00, 4, true
) ON CONFLICT (sku) DO NOTHING;

-- Producto 5 (Servicio): Impresión e Instalación de Sticker Parasol
INSERT INTO products (id, category_id, brand_id, sku, name, description, product_type, unit, track_stock, published, active)
VALUES (
    '00000000-0000-0000-000b-000000000005',
    '00000000-0000-0000-0009-000000000005',
    '00000000-0000-0000-000a-000000000005',
    'MEGA-SRV-STK-PARASOL',
    'Servicio: Diseño, Impresión e Instalación Sticker Parasol',
    'Franja parasol personalizada en vinilo premium americano con corte computarizado e instalación profesional anti-burbujas.',
    'SERVICE', 'SERVICE', false, true, true
) ON CONFLICT (sku) DO NOTHING;

INSERT INTO product_variants (id, product_id, sku, barcode, attributes, sale_price, cost_price, min_stock, active)
VALUES (
    '00000000-0000-0000-000c-000000000005',
    '00000000-0000-0000-000b-000000000005',
    'MEGA-SRV-STK-PARASOL-STD',
    '7860000000059',
    '{"service": "Parasol Vinilo", "warranty": "6 meses"}',
    20.00, 6.00, 0, true
) ON CONFLICT (sku) DO NOTHING;

-- 12. Existencias Iniciales (Bodega Principal)
INSERT INTO stock_balances (variant_id, warehouse_id, quantity, reserved, version)
VALUES
    ('00000000-0000-0000-000c-000000000001', '00000000-0000-0000-0002-000000000001', 25.000, 0, 0),
    ('00000000-0000-0000-000c-000000000002', '00000000-0000-0000-0002-000000000001', 10.000, 0, 0),
    ('00000000-0000-0000-000c-000000000003', '00000000-0000-0000-0002-000000000001', 18.000, 0, 0),
    ('00000000-0000-0000-000c-000000000004', '00000000-0000-0000-0002-000000000001', 14.000, 0, 0)
ON CONFLICT (variant_id, warehouse_id) DO NOTHING;

-- Movimientos Kardex iniciales
INSERT INTO stock_movements (variant_id, warehouse_id, movement_type, quantity_delta, balance_after, unit_cost, source_type, source_id, notes)
VALUES
    ('00000000-0000-0000-000c-000000000001', '00000000-0000-0000-0002-000000000001', 'INITIAL_STOCK', 25.000, 25.000, 18.50, 'INVENTORY_INIT', '00000000-0000-0000-0002-000000000001', 'Inventario inicial de apertura'),
    ('00000000-0000-0000-000c-000000000002', '00000000-0000-0000-0002-000000000001', 'INITIAL_STOCK', 10.000, 10.000, 85.00, 'INVENTORY_INIT', '00000000-0000-0000-0002-000000000001', 'Inventario inicial de apertura'),
    ('00000000-0000-0000-000c-000000000003', '00000000-0000-0000-0002-000000000001', 'INITIAL_STOCK', 18.000, 18.000, 32.00, 'INVENTORY_INIT', '00000000-0000-0000-0002-000000000001', 'Inventario inicial de apertura'),
    ('00000000-0000-0000-000c-000000000004', '00000000-0000-0000-0002-000000000001', 'INITIAL_STOCK', 14.000, 14.000, 38.00, 'INVENTORY_INIT', '00000000-0000-0000-0002-000000000001', 'Inventario inicial de apertura')
ON CONFLICT DO NOTHING;

-- 13. Vitrina y Showcase para Home y Modo TV
INSERT INTO showcases (id, code, title, description, autoplay_seconds, active)
VALUES (
    '00000000-0000-0000-000d-000000000001',
    'HOME_FEATURED',
    'Lo Más Destacado en Megalujos',
    'Novedades y accesorios automotrices más cotizados de la semana',
    6,
    true
) ON CONFLICT (code) DO NOTHING;

INSERT INTO showcase_products (showcase_id, variant_id, sort_order)
VALUES
    ('00000000-0000-0000-000d-000000000001', '00000000-0000-0000-000c-000000000001', 1),
    ('00000000-0000-0000-000d-000000000001', '00000000-0000-0000-000c-000000000002', 2),
    ('00000000-0000-0000-000d-000000000001', '00000000-0000-0000-000c-000000000003', 3),
    ('00000000-0000-0000-000d-000000000001', '00000000-0000-0000-000c-000000000004', 4)
ON CONFLICT DO NOTHING;
