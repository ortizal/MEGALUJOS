CREATE TABLE stock_balances (
    variant_id uuid NOT NULL REFERENCES product_variants(id) ON DELETE CASCADE,
    warehouse_id uuid NOT NULL REFERENCES warehouses(id) ON DELETE CASCADE,
    quantity numeric(18,3) NOT NULL DEFAULT 0 CHECK(quantity >= 0),
    reserved numeric(18,3) NOT NULL DEFAULT 0 CHECK(reserved >= 0 AND reserved <= quantity),
    version bigint NOT NULL DEFAULT 0,
    updated_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY(variant_id, warehouse_id)
);

CREATE TABLE stock_movements (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    variant_id uuid NOT NULL REFERENCES product_variants(id) ON DELETE RESTRICT,
    warehouse_id uuid NOT NULL REFERENCES warehouses(id) ON DELETE RESTRICT,
    movement_type varchar(30) NOT NULL CHECK(movement_type IN (
        'PURCHASE', 'SALE', 'SALE_RETURN', 'SUPPLIER_RETURN', 
        'ADJUSTMENT_IN', 'ADJUSTMENT_OUT', 'SERVICE_CONSUMPTION', 'SERVICE_RETURN', 'INITIAL_STOCK'
    )),
    quantity_delta numeric(18,3) NOT NULL CHECK(quantity_delta <> 0),
    balance_after numeric(18,3),
    unit_cost numeric(18,4) CHECK(unit_cost IS NULL OR unit_cost >= 0),
    source_type varchar(60) NOT NULL,
    source_id uuid NOT NULL,
    source_line_id uuid,
    actor_id uuid REFERENCES users(id),
    notes text,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX ix_stock_movements_variant_date ON stock_movements(variant_id, created_at DESC);
CREATE INDEX ix_stock_movements_source ON stock_movements(source_type, source_id);
CREATE INDEX ix_stock_movements_warehouse ON stock_movements(warehouse_id, created_at DESC);

-- Ajustes de inventario formales
CREATE TABLE inventory_adjustments (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    adjustment_number varchar(40) NOT NULL UNIQUE,
    warehouse_id uuid NOT NULL REFERENCES warehouses(id),
    status varchar(30) NOT NULL CHECK(status IN ('DRAFT', 'APPROVED', 'CANCELLED')),
    reason text NOT NULL,
    created_by uuid NOT NULL REFERENCES users(id),
    approved_by uuid REFERENCES users(id),
    created_at timestamptz NOT NULL DEFAULT now(),
    approved_at timestamptz
);

CREATE TABLE inventory_adjustment_items (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    adjustment_id uuid NOT NULL REFERENCES inventory_adjustments(id) ON DELETE CASCADE,
    variant_id uuid NOT NULL REFERENCES product_variants(id),
    current_quantity numeric(18,3) NOT NULL,
    adjusted_quantity numeric(18,3) NOT NULL,
    difference numeric(18,3) NOT NULL,
    cost numeric(18,4)
);
