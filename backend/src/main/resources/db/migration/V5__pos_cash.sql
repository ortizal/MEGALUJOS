CREATE TABLE cash_registers (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id uuid NOT NULL REFERENCES branches(id),
    code varchar(30) NOT NULL,
    name varchar(100) NOT NULL,
    active boolean NOT NULL DEFAULT true,
    UNIQUE(branch_id, code)
);

CREATE TABLE cash_sessions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    register_id uuid NOT NULL REFERENCES cash_registers(id),
    opened_by uuid NOT NULL REFERENCES users(id),
    closed_by uuid REFERENCES users(id),
    opened_at timestamptz NOT NULL DEFAULT now(),
    closed_at timestamptz,
    opening_amount numeric(18,2) NOT NULL CHECK(opening_amount >= 0),
    counted_amount numeric(18,2),
    expected_amount numeric(18,2),
    difference_amount numeric(18,2),
    status varchar(20) NOT NULL CHECK(status IN ('OPEN', 'CLOSED')),
    notes text
);

CREATE UNIQUE INDEX ux_open_cash_session ON cash_sessions(register_id) WHERE status = 'OPEN';

CREATE TABLE sales (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    sale_number varchar(40) NOT NULL UNIQUE,
    customer_id uuid REFERENCES customers(id),
    cash_session_id uuid NOT NULL REFERENCES cash_sessions(id),
    status varchar(30) NOT NULL CHECK(status IN ('DRAFT', 'CONFIRMED', 'RETURNED', 'PARTIALLY_RETURNED', 'VOIDED')),
    subtotal numeric(18,2) NOT NULL DEFAULT 0,
    discount_total numeric(18,2) NOT NULL DEFAULT 0,
    tax_total numeric(18,2) NOT NULL DEFAULT 0,
    grand_total numeric(18,2) NOT NULL DEFAULT 0,
    idempotency_key uuid NOT NULL UNIQUE,
    notes text,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE sale_items (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    sale_id uuid NOT NULL REFERENCES sales(id) ON DELETE CASCADE,
    variant_id uuid REFERENCES product_variants(id),
    service_order_id uuid, -- La foreign key se creará en V6__services
    description varchar(255) NOT NULL,
    quantity numeric(18,3) NOT NULL CHECK(quantity > 0),
    unit_price numeric(18,4) NOT NULL CHECK(unit_price >= 0),
    discount numeric(18,2) NOT NULL DEFAULT 0,
    tax_code varchar(30) NOT NULL DEFAULT 'IVA_15',
    tax_rate numeric(9,5) NOT NULL DEFAULT 0.15,
    tax_amount numeric(18,2) NOT NULL DEFAULT 0,
    line_total numeric(18,2) NOT NULL
);

CREATE TABLE sale_payments (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    sale_id uuid NOT NULL REFERENCES sales(id) ON DELETE CASCADE,
    method varchar(30) NOT NULL CHECK(method IN ('CASH', 'TRANSFER', 'CARD', 'OTHER', 'APPLIED_ADVANCE')),
    amount numeric(18,2) NOT NULL CHECK(amount > 0),
    reference varchar(120),
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE cash_movements (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id uuid NOT NULL REFERENCES cash_sessions(id) ON DELETE CASCADE,
    movement_type varchar(30) NOT NULL CHECK(movement_type IN ('INCOME', 'EXPENSE', 'SALE_PAYMENT', 'ADVANCE_PAYMENT', 'REFUND')),
    amount numeric(18,2) NOT NULL CHECK(amount > 0),
    source_type varchar(50),
    source_id uuid,
    actor_id uuid REFERENCES users(id),
    reason varchar(255),
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE sale_returns (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    return_number varchar(40) NOT NULL UNIQUE,
    sale_id uuid NOT NULL REFERENCES sales(id),
    cash_session_id uuid NOT NULL REFERENCES cash_sessions(id),
    total_returned numeric(18,2) NOT NULL CHECK(total_returned > 0),
    reason text NOT NULL,
    authorized_by uuid NOT NULL REFERENCES users(id),
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE sale_return_items (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    sale_return_id uuid NOT NULL REFERENCES sale_returns(id) ON DELETE CASCADE,
    sale_item_id uuid NOT NULL REFERENCES sale_items(id),
    quantity numeric(18,3) NOT NULL CHECK(quantity > 0),
    return_amount numeric(18,2) NOT NULL,
    restock boolean NOT NULL DEFAULT true
);
