-- ============================================================
-- V17__add_sale_void_movement_type.sql
-- Anulación de venta (void) reintegra stock con trazabilidad
-- en kardex usando movement_type 'SALE_VOID', que no estaba
-- contemplado en el CHECK constraint original de V3.
-- ============================================================

ALTER TABLE stock_movements DROP CONSTRAINT stock_movements_movement_type_check;

ALTER TABLE stock_movements ADD CONSTRAINT stock_movements_movement_type_check
    CHECK (movement_type IN (
        'PURCHASE', 'SALE', 'SALE_VOID', 'SALE_RETURN', 'SUPPLIER_RETURN',
        'ADJUSTMENT_IN', 'ADJUSTMENT_OUT', 'SERVICE_CONSUMPTION', 'SERVICE_RETURN', 'INITIAL_STOCK'
    ));
