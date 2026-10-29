CREATE INDEX IF NOT EXISTS ix_products_category_active ON products(category_id, active);
CREATE INDEX IF NOT EXISTS ix_products_published ON products(published) WHERE active;
CREATE INDEX IF NOT EXISTS ix_sales_created_at ON sales(created_at DESC);
CREATE INDEX IF NOT EXISTS ix_sales_cash_session ON sales(cash_session_id);
CREATE INDEX IF NOT EXISTS ix_service_orders_status ON service_orders(status, scheduled_at);
CREATE INDEX IF NOT EXISTS ix_electronic_documents_status ON electronic_documents(status, created_at);
CREATE INDEX IF NOT EXISTS ix_cash_movements_session ON cash_movements(session_id, created_at);
CREATE INDEX IF NOT EXISTS ix_product_variants_product ON product_variants(product_id);

-- Vista optimizada para catálogo público (solo productos activos y publicados)
CREATE OR REPLACE VIEW v_public_catalog AS
SELECT 
    p.id AS product_id,
    p.sku AS product_sku,
    p.name AS product_name,
    p.description AS product_description,
    p.product_type,
    p.unit,
    c.id AS category_id,
    c.name AS category_name,
    c.slug AS category_slug,
    b.id AS brand_id,
    b.name AS brand_name,
    pv.id AS variant_id,
    pv.sku AS variant_sku,
    pv.barcode,
    pv.attributes,
    pv.sale_price,
    COALESCE(sb.quantity, 0) AS physical_stock,
    COALESCE(sb.reserved, 0) AS reserved_stock,
    GREATEST(0, COALESCE(sb.quantity, 0) - COALESCE(sb.reserved, 0)) AS available_stock,
    CASE 
        WHEN p.track_stock = false THEN 'AVAILABLE'
        WHEN GREATEST(0, COALESCE(sb.quantity, 0) - COALESCE(sb.reserved, 0)) > pv.min_stock THEN 'AVAILABLE'
        WHEN GREATEST(0, COALESCE(sb.quantity, 0) - COALESCE(sb.reserved, 0)) > 0 THEN 'LOW_STOCK'
        ELSE 'OUT_OF_STOCK'
    END AS availability_status,
    (
        SELECT mf.storage_key 
        FROM product_images pi 
        JOIN media_files mf ON pi.media_id = mf.id 
        WHERE pi.product_id = p.id 
        ORDER BY pi.is_primary DESC, pi.sort_order ASC 
        LIMIT 1
    ) AS primary_image_key
FROM products p
JOIN categories c ON p.category_id = c.id
LEFT JOIN brands b ON p.brand_id = b.id
JOIN product_variants pv ON pv.product_id = p.id AND pv.active = true
LEFT JOIN stock_balances sb ON sb.variant_id = pv.id
WHERE p.active = true AND p.published = true AND c.active = true;
