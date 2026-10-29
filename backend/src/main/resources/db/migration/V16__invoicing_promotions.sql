-- ============================================================
-- V16 - Promociones de vitrina y extensiones de facturación electrónica
-- ============================================================

-- 1. Promociones configurables por producto o categoría
CREATE TABLE promotions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name varchar(150) NOT NULL,
    description text,
    promo_type varchar(20) NOT NULL CHECK(promo_type IN ('PERCENT', 'AMOUNT', 'FIXED_PRICE')),
    value numeric(18,2) NOT NULL CHECK(value >= 0),
    starts_at timestamptz NOT NULL,
    ends_at timestamptz NOT NULL,
    active boolean NOT NULL DEFAULT true,
    product_id uuid REFERENCES products(id) ON DELETE SET NULL,
    category_id uuid REFERENCES categories(id) ON DELETE SET NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CHECK(ends_at > starts_at)
);

CREATE INDEX ix_promotions_active ON promotions(active, starts_at, ends_at);

-- 2. Notas de crédito: referencia a factura original y motivo
ALTER TABLE electronic_documents ADD COLUMN IF NOT EXISTS reference_document_id uuid;
ALTER TABLE electronic_documents ADD COLUMN IF NOT EXISTS reason text;

-- 3. Idempotencia de emisión (cabecera Idempotency-Key persistida por documento)
ALTER TABLE electronic_documents ADD COLUMN IF NOT EXISTS idempotency_key uuid;
CREATE UNIQUE INDEX IF NOT EXISTS ux_electronic_documents_idempotency
    ON electronic_documents(idempotency_key) WHERE idempotency_key IS NOT NULL;
