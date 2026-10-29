import { Decimal, IsoDateTime, Uuid } from './common.model';
import { PaymentMethodCode } from './enums.model';

/** Línea de venta al confirmar (el servidor recalcula precios e impuestos). */
export interface ConfirmSaleItemRequest {
  variantId: Uuid;
  quantity: Decimal;
  /** Descuento de la línea (nombre de la cabecera HTTP/backend). */
  discount?: Decimal;
  /** Alias del contrato maestro (`discountAmount`); el api-client lo mapea a `discount`. */
  discountAmount?: Decimal;
}

/** Pago de venta al confirmar. */
export interface ConfirmSalePaymentRequest {
  method: PaymentMethodCode | string;
  amount: Decimal;
  reference?: string | null;
}

/** Confirmación de venta (Idempotency-Key va en cabecera HTTP). */
export interface ConfirmSaleRequest {
  cashSessionId: Uuid;
  customerId?: Uuid | null;
  items: ConfirmSaleItemRequest[];
  payments: ConfirmSalePaymentRequest[];
  requestInvoice?: boolean;
  notes?: string | null;
}

/** Ítem de venta (respuesta). */
export interface SaleItemDto {
  id: Uuid;
  variantId?: Uuid | null;
  description: string;
  quantity: Decimal;
  unitPrice: Decimal;
  discount: Decimal;
  taxCode?: string | null;
  taxRate?: Decimal | null;
  taxAmount: Decimal;
  lineTotal: Decimal;
}

/** Pago de venta (respuesta). */
export interface SalePaymentDto {
  id?: Uuid | null;
  method: PaymentMethodCode | string;
  amount: Decimal;
  reference?: string | null;
}

/** Venta confirmada (respuesta completa). */
export interface SaleResponseDto {
  saleId: Uuid;
  saleNumber: string;
  status: string;
  subtotal: Decimal;
  discountTotal: Decimal;
  taxTotal: Decimal;
  grandTotal: Decimal;
  idempotencyKey?: Uuid | null;
  createdAt?: IsoDateTime | null;
  invoiceStatus?: string | null;
  items: SaleItemDto[];
  payments: SalePaymentDto[];
}

/** Venta extendida con cliente, caja e historial de devoluciones. */
export interface SaleDetailDto extends SaleResponseDto {
  customerId?: Uuid | null;
  cashSessionId?: Uuid | null;
  totalReturned?: Decimal;
  returns?: SaleReturnDto[];
}

/** Resumen de venta para el historial paginado. */
export interface SaleSummaryDto {
  /** Alias del id de venta (lo normaliza el api-client; `saleId` es el campo del backend). */
  id: Uuid;
  saleId: Uuid;
  saleNumber: string;
  customerId?: Uuid | null;
  customerName?: string | null;
  cashSessionId?: Uuid | null;
  status: string;
  subtotal: Decimal;
  discountTotal: Decimal;
  taxTotal: Decimal;
  grandTotal: Decimal;
  totalReturned?: Decimal;
  createdAt?: IsoDateTime | null;
  /** Nunca se usa: impide confundir un resumen con un detalle de venta. */
  sale?: never;
}

/** Ítem de devolución solicitado. */
export interface SaleReturnItemRequest {
  saleItemId: Uuid;
  quantity: Decimal;
  restock?: boolean;
}

/** Solicitud de devolución de venta. */
export interface SaleReturnRequest {
  reason: string;
  items: SaleReturnItemRequest[];
  paymentMethod?: PaymentMethodCode | string;
}

/** Ítem de devolución (respuesta). */
export interface SaleReturnItemDto {
  id: Uuid;
  saleItemId: Uuid;
  quantity: Decimal;
  returnAmount: Decimal;
  restock?: boolean | null;
}

/** Devolución de venta (respuesta). */
export interface SaleReturnDto {
  id: Uuid;
  returnNumber: string;
  saleId: Uuid;
  cashSessionId?: Uuid | null;
  totalReturned: Decimal;
  reason?: string | null;
  paymentMethod?: string | null;
  authorizedBy?: Uuid | null;
  createdAt: IsoDateTime;
  items: SaleReturnItemDto[];
}
