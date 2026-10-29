CREATE TABLE categories (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_id uuid REFERENCES categories(id) ON DELETE SET NULL,
    name varchar(150) NOT NULL,
    slug varchar(180) NOT NULL UNIQUE,
    description text,
    sort_order integer NOT NULL DEFAULT 0,
    active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE brands (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name varchar(150) NOT NULL UNIQUE,
    logo_media_id uuid,
    active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE products (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id uuid NOT NULL REFERENCES categories(id),
    brand_id uuid REFERENCES brands(id),
    sku varchar(60) NOT NULL UNIQUE,
    name varchar(200) NOT NULL,
    description text,
    product_type varchar(20) NOT NULL CHECK(product_type IN ('PHYSICAL', 'SERVICE')),
    unit varchar(20) NOT NULL DEFAULT 'UNIT',
    track_stock boolean NOT NULL DEFAULT true,
    published boolean NOT NULL DEFAULT false,
    active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CHECK(product_type <> 'SERVICE' OR track_stock = false)
);

CREATE TABLE product_variants (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id uuid NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    sku varchar(80) NOT NULL UNIQUE,
    barcode varchar(80) UNIQUE,
    attributes jsonb NOT NULL DEFAULT '{}'::jsonb,
    sale_price numeric(18,2) NOT NULL CHECK(sale_price >= 0),
    cost_price numeric(18,4) CHECK(cost_price IS NULL OR cost_price >= 0),
    min_stock numeric(18,3) NOT NULL DEFAULT 0 CHECK(min_stock >= 0),
    active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE media_files (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    storage_key text NOT NULL UNIQUE,
    original_name varchar(255) NOT NULL,
    content_type varchar(100) NOT NULL,
    size_bytes bigint NOT NULL CHECK(size_bytes > 0),
    sha256 varchar(64),
    is_public boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE product_images (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id uuid NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    media_id uuid NOT NULL REFERENCES media_files(id) ON DELETE CASCADE,
    sort_order integer NOT NULL DEFAULT 0,
    is_primary boolean NOT NULL DEFAULT false,
    UNIQUE(product_id, media_id)
);

CREATE UNIQUE INDEX ux_product_primary_image ON product_images(product_id) WHERE is_primary;

CREATE TABLE vehicle_brands (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name varchar(120) NOT NULL UNIQUE
);

CREATE TABLE vehicle_models (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    brand_id uuid NOT NULL REFERENCES vehicle_brands(id) ON DELETE CASCADE,
    name varchar(120) NOT NULL,
    UNIQUE(brand_id, name)
);

CREATE TABLE vehicle_versions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    model_id uuid NOT NULL REFERENCES vehicle_models(id) ON DELETE CASCADE,
    name varchar(120) NOT NULL,
    year_from integer,
    year_to integer,
    CHECK(year_to IS NULL OR year_from IS NULL OR year_to >= year_from)
);

CREATE TABLE product_vehicle_compatibility (
    variant_id uuid NOT NULL REFERENCES product_variants(id) ON DELETE CASCADE,
    vehicle_version_id uuid NOT NULL REFERENCES vehicle_versions(id) ON DELETE CASCADE,
    verified boolean NOT NULL DEFAULT false,
    notes text,
    PRIMARY KEY(variant_id, vehicle_version_id)
);
