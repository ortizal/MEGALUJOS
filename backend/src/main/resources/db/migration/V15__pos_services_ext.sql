-- ============================================================
-- V15 - Extensiones POS / Caja / Servicios (agente pos/cash/services)
-- Solo se agregan elementos que V5, V6 y V11 no contemplaban.
-- ============================================================

-- 1. Medio de pago con el que se devolvió el dinero (constancia de la devolución)
ALTER TABLE sale_returns ADD COLUMN payment_method varchar(30) NOT NULL DEFAULT 'CASH';

-- 2. Índices para historial de ventas y validación de devoluciones
CREATE INDEX IF NOT EXISTS ix_sale_return_items_sale_item ON sale_return_items(sale_item_id);
CREATE INDEX IF NOT EXISTS ix_sale_returns_sale ON sale_returns(sale_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_sales_status_created ON sales(status, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_sales_customer ON sales(customer_id);

-- 3. Índices para órdenes de servicio (agenda, materiales y anticipos)
CREATE INDEX IF NOT EXISTS ix_service_materials_order ON service_materials(service_order_id);
CREATE INDEX IF NOT EXISTS ix_service_advances_order ON service_advances(service_order_id);
CREATE INDEX IF NOT EXISTS ix_service_advances_session ON service_advances(cash_session_id);
CREATE INDEX IF NOT EXISTS ix_service_orders_scheduled ON service_orders(scheduled_at) WHERE scheduled_at IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_service_orders_assigned ON service_orders(assigned_user_id);
