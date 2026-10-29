CREATE TABLE service_orders (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number varchar(40) NOT NULL UNIQUE,
    customer_id uuid NOT NULL REFERENCES customers(id),
    vehicle_id uuid REFERENCES customer_vehicles(id),
    service_type varchar(40) NOT NULL CHECK(service_type IN ('STICKER', 'INSTALLATION', 'OTHER')),
    description text,
    dimensions varchar(100), -- p.ej. "90x15 cm"
    status varchar(30) NOT NULL CHECK(status IN (
        'QUOTED', 'CONFIRMED', 'IN_PROGRESS', 'READY_FOR_DELIVERY', 'DELIVERED', 'CANCELLED'
    )),
    assigned_user_id uuid REFERENCES users(id),
    labor_amount numeric(18,2) NOT NULL DEFAULT 0,
    estimated_total numeric(18,2) NOT NULL DEFAULT 0,
    scheduled_at timestamptz,
    completed_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE sale_items 
    ADD CONSTRAINT fk_sale_item_service_order 
    FOREIGN KEY(service_order_id) REFERENCES service_orders(id) ON DELETE SET NULL;

CREATE TABLE service_order_items (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    service_order_id uuid NOT NULL REFERENCES service_orders(id) ON DELETE CASCADE,
    description varchar(255) NOT NULL,
    quantity numeric(18,3) NOT NULL CHECK(quantity > 0),
    unit_price numeric(18,2) NOT NULL CHECK(unit_price >= 0),
    line_total numeric(18,2) NOT NULL
);

CREATE TABLE service_materials (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    service_order_id uuid NOT NULL REFERENCES service_orders(id) ON DELETE CASCADE,
    variant_id uuid NOT NULL REFERENCES product_variants(id),
    planned_quantity numeric(18,3) NOT NULL DEFAULT 0,
    consumed_quantity numeric(18,3) NOT NULL DEFAULT 0,
    CHECK(planned_quantity >= 0 AND consumed_quantity >= 0)
);

CREATE TABLE service_attachments (
    service_order_id uuid NOT NULL REFERENCES service_orders(id) ON DELETE CASCADE,
    media_id uuid NOT NULL REFERENCES media_files(id) ON DELETE CASCADE,
    attachment_type varchar(40) DEFAULT 'DESIGN',
    PRIMARY KEY(service_order_id, media_id)
);

CREATE TABLE service_status_history (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    service_order_id uuid NOT NULL REFERENCES service_orders(id) ON DELETE CASCADE,
    old_status varchar(30),
    new_status varchar(30) NOT NULL,
    actor_id uuid REFERENCES users(id),
    changed_at timestamptz NOT NULL DEFAULT now(),
    notes text
);

CREATE TABLE service_advances (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    service_order_id uuid NOT NULL REFERENCES service_orders(id) ON DELETE CASCADE,
    cash_session_id uuid NOT NULL REFERENCES cash_sessions(id),
    amount numeric(18,2) NOT NULL CHECK(amount > 0),
    applied_amount numeric(18,2) NOT NULL DEFAULT 0,
    payment_method varchar(30) NOT NULL DEFAULT 'CASH',
    reference varchar(100),
    created_at timestamptz NOT NULL DEFAULT now(),
    CHECK(applied_amount >= 0 AND applied_amount <= amount)
);
