CREATE TABLE customers (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    document_type varchar(20) NOT NULL CHECK(document_type IN ('CEDULA', 'RUC', 'PASAPORTE', 'CONSUMIDOR_FINAL')),
    document_number varchar(30),
    name varchar(200) NOT NULL,
    email varchar(200),
    phone varchar(40),
    address text,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_customer_document ON customers(document_type, document_number)
    WHERE document_number IS NOT NULL AND document_type <> 'CONSUMIDOR_FINAL';

CREATE TABLE customer_vehicles (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id uuid NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    plate varchar(20),
    vehicle_version_id uuid REFERENCES vehicle_versions(id) ON DELETE SET NULL,
    brand_name varchar(100),
    model_name varchar(100),
    year integer,
    color varchar(60),
    notes text,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX ix_customer_vehicles_plate ON customer_vehicles(plate);

CREATE TABLE suppliers (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tax_id varchar(30) UNIQUE,
    name varchar(200) NOT NULL,
    contact_name varchar(150),
    email varchar(200),
    phone varchar(40),
    address text,
    active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE purchase_orders (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    supplier_id uuid NOT NULL REFERENCES suppliers(id),
    order_number varchar(40) NOT NULL UNIQUE,
    status varchar(30) NOT NULL CHECK(status IN ('DRAFT', 'ORDERED', 'PARTIALLY_RECEIVED', 'RECEIVED', 'CANCELLED')),
    ordered_at timestamptz NOT NULL DEFAULT now(),
    subtotal numeric(18,2) NOT NULL DEFAULT 0,
    tax_total numeric(18,2) NOT NULL DEFAULT 0,
    total numeric(18,2) NOT NULL DEFAULT 0,
    notes text
);

CREATE TABLE purchase_order_items (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id uuid NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    variant_id uuid NOT NULL REFERENCES product_variants(id),
    quantity numeric(18,3) NOT NULL CHECK(quantity > 0),
    unit_cost numeric(18,4) NOT NULL CHECK(unit_cost >= 0),
    line_total numeric(18,2) NOT NULL DEFAULT 0
);

CREATE TABLE goods_receipts (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id uuid NOT NULL REFERENCES purchase_orders(id),
    warehouse_id uuid NOT NULL REFERENCES warehouses(id),
    receipt_number varchar(40) NOT NULL UNIQUE,
    received_at timestamptz NOT NULL DEFAULT now(),
    status varchar(30) NOT NULL CHECK(status IN ('RECEIVED', 'VERIFIED', 'CANCELLED')),
    notes text
);

CREATE TABLE goods_receipt_items (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    receipt_id uuid NOT NULL REFERENCES goods_receipts(id) ON DELETE CASCADE,
    order_item_id uuid NOT NULL REFERENCES purchase_order_items(id),
    quantity numeric(18,3) NOT NULL CHECK(quantity > 0)
);
