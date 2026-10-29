/** Códigos de disponibilidad pública de un producto/variante. */
export const STOCK_AVAILABILITY_CODES = [
  'AVAILABLE',
  'LOW_STOCK',
  'OUT_OF_STOCK',
  'ON_REQUEST',
] as const;
export type StockAvailability = (typeof STOCK_AVAILABILITY_CODES)[number];

/** Etiquetas en español para disponibilidad. */
export const STOCK_AVAILABILITY_LABELS: Record<StockAvailability, string> = {
  AVAILABLE: 'Disponible',
  LOW_STOCK: 'Stock bajo',
  OUT_OF_STOCK: 'Agotado',
  ON_REQUEST: 'Bajo pedido',
};

/** Métodos de pago admitidos por caja/POS. */
export const PAYMENT_METHOD_CODES = [
  'CASH',
  'TRANSFER',
  'CARD',
  'OTHER',
  'APPLIED_ADVANCE',
] as const;
export type PaymentMethodCode = (typeof PAYMENT_METHOD_CODES)[number];

export const PAYMENT_METHOD_LABELS: Record<PaymentMethodCode, string> = {
  CASH: 'Efectivo',
  TRANSFER: 'Transferencia',
  CARD: 'Tarjeta',
  OTHER: 'Otro',
  APPLIED_ADVANCE: 'Anticipo aplicado',
};

/** Estados de venta. */
export const SALE_STATUS_CODES = [
  'PENDING',
  'PAID',
  'PARTIALLY_RETURNED',
  'RETURNED',
  'VOID',
] as const;
export type SaleStatusCode = (typeof SALE_STATUS_CODES)[number];

export const SALE_STATUS_LABELS: Record<SaleStatusCode, string> = {
  PENDING: 'Pendiente',
  PAID: 'Pagada',
  PARTIALLY_RETURNED: 'Devuelta parcialmente',
  RETURNED: 'Devuelta',
  VOID: 'Anulada',
};

/** Estados de documento electrónico. */
export const ELECTRONIC_DOCUMENT_STATUS_CODES = [
  'PENDING',
  'PROCESSING',
  'AUTHORIZED',
  'REJECTED',
  'ERROR',
  'CREDIT_NOTE',
] as const;
export type ElectronicDocumentStatus = (typeof ELECTRONIC_DOCUMENT_STATUS_CODES)[number];

export const ELECTRONIC_DOCUMENT_STATUS_LABELS: Record<ElectronicDocumentStatus, string> = {
  PENDING: 'Pendiente',
  PROCESSING: 'Procesando',
  AUTHORIZED: 'Autorizado',
  REJECTED: 'Rechazado',
  ERROR: 'Con error',
  CREDIT_NOTE: 'Nota de crédito',
};

/** Estados de sesión de caja. */
export const CASH_SESSION_STATUS_CODES = ['OPEN', 'CLOSED'] as const;
export type CashSessionStatus = (typeof CASH_SESSION_STATUS_CODES)[number];

export const CASH_SESSION_STATUS_LABELS: Record<CashSessionStatus, string> = {
  OPEN: 'Abierta',
  CLOSED: 'Cerrada',
};

/** Estados de orden de servicio. */
export const SERVICE_ORDER_STATUS_CODES = [
  'QUOTATION',
  'CONFIRMED',
  'IN_PROGRESS',
  'READY',
  'DELIVERED',
  'CANCELLED',
] as const;
export type ServiceOrderStatusCode = (typeof SERVICE_ORDER_STATUS_CODES)[number];

export const SERVICE_ORDER_STATUS_LABELS: Record<ServiceOrderStatusCode, string> = {
  QUOTATION: 'Cotización',
  CONFIRMED: 'Confirmada',
  IN_PROGRESS: 'En ejecución',
  READY: 'Lista',
  DELIVERED: 'Entregada',
  CANCELLED: 'Cancelada',
};

/** Tipos de orden de servicio. */
export type ServiceTypeCode = 'STICKER' | 'INSTALLATION' | 'OTHER';

export const SERVICE_TYPE_LABELS: Record<ServiceTypeCode, string> = {
  STICKER: 'Sticker',
  INSTALLATION: 'Instalación',
  OTHER: 'Otro',
};

/** Tipos de documento de identidad de clientes. */
export const CUSTOMER_DOCUMENT_TYPES = [
  'CEDULA',
  'RUC',
  'PASAPORTE',
  'CONSUMIDOR_FINAL',
] as const;
export type CustomerDocumentType = (typeof CUSTOMER_DOCUMENT_TYPES)[number];

/** Estados de orden de compra. */
export const PURCHASE_ORDER_STATUS_CODES = [
  'DRAFT',
  'ORDERED',
  'PARTIALLY_RECEIVED',
  'RECEIVED',
  'CANCELLED',
] as const;
export type PurchaseOrderStatusCode = (typeof PURCHASE_ORDER_STATUS_CODES)[number];

export const PURCHASE_ORDER_STATUS_LABELS: Record<PurchaseOrderStatusCode, string> = {
  DRAFT: 'Borrador',
  ORDERED: 'Ordenada',
  PARTIALLY_RECEIVED: 'Recepción parcial',
  RECEIVED: 'Recibida',
  CANCELLED: 'Cancelada',
};
