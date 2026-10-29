CREATE TABLE banners (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    title varchar(180) NOT NULL,
    subtitle varchar(255),
    media_id uuid NOT NULL REFERENCES media_files(id),
    link_url text,
    starts_at timestamptz,
    ends_at timestamptz,
    sort_order integer NOT NULL DEFAULT 0,
    active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE showcases (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code varchar(60) NOT NULL UNIQUE,
    title varchar(180) NOT NULL,
    description text,
    autoplay_seconds integer NOT NULL DEFAULT 7 CHECK(autoplay_seconds BETWEEN 3 AND 60),
    active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE showcase_products (
    showcase_id uuid NOT NULL REFERENCES showcases(id) ON DELETE CASCADE,
    variant_id uuid NOT NULL REFERENCES product_variants(id) ON DELETE CASCADE,
    sort_order integer NOT NULL DEFAULT 0,
    PRIMARY KEY(showcase_id, variant_id)
);

CREATE TABLE catalog_inquiries (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    variant_id uuid REFERENCES product_variants(id) ON DELETE SET NULL,
    source varchar(30) NOT NULL DEFAULT 'WHATSAPP',
    customer_phone varchar(40),
    user_agent text,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX ix_catalog_inquiries_time ON catalog_inquiries(created_at DESC);
