import { Decimal, IsoDateTime, Uuid } from './common.model';
import { PaymentMethodCode, ServiceTypeCode } from './enums.model';

/** Ítem de mano de obra de una orden de servicio. */
export interface ServiceOrderItemDto {
  id: Uuid;
  description: string;
  quantity: Decimal;
  unitPrice: Decimal;
  lineTotal: Decimal;
}

/** Solicitud de ítem de orden de servicio. */
export interface ServiceOrderItemRequest {
  description: string;
  quantity: Decimal;
  unitPrice: Decimal;
}

/** Material planificado/consumido de una orden. */
export interface ServiceMaterialDto {
  id: Uuid;
  variantId: Uuid;
  plannedQuantity: Decimal;
  consumedQuantity: Decimal;
  sku?: string | null;
  productName?: string | null;
}

/** Solicitud de material planificado. */
export interface ServiceMaterialRequest {
  variantId: Uuid;
  plannedQuantity: Decimal;
}

/** Cambio de estado de una orden. */
export interface ServiceStatusHistoryDto {
  id: Uuid;
  oldStatus?: string | null;
  newStatus: string;
  actorId?: Uuid | null;
  changedAt: IsoDateTime;
  notes?: string | null;
}

/** Anticipo registrado sobre una orden. */
export interface ServiceAdvanceDto {
  id: Uuid;
  cashSessionId?: Uuid | null;
  amount: Decimal;
  appliedAmount?: Decimal;
  paymentMethod?: PaymentMethodCode | string | null;
  reference?: string | null;
  createdAt?: IsoDateTime | null;
}

/** Solicitud de anticipo. */
export interface CreateAdvanceRequest {
  amount: Decimal;
  cashSessionId?: Uuid | null;
  paymentMethod: PaymentMethodCode | string;
  reference?: string | null;
}

/** Adjunto de una orden de servicio. */
export interface ServiceAttachmentDto {
  mediaId: Uuid;
  attachmentType?: string | null;
  storageKey?: string | null;
  originalName?: string | null;
  contentType?: string | null;
  sizeBytes?: number | null;
  url?: string | null;
}

/** Orden de servicio (detalle). */
export interface ServiceOrderDto {
  id: Uuid;
  orderNumber: string;
  customerId: Uuid;
  vehicleId?: Uuid | null;
  serviceType: ServiceTypeCode | string;
  description: string;
  dimensions?: string | null;
  status: string;
  assignedUserId?: Uuid | null;
  assignedUserName?: string | null;
  laborAmount: Decimal;
  estimatedTotal: Decimal;
  advancesTotal?: Decimal;
  /** Anticipos aplicados (alias del contrato maestro; lo normaliza ServicesService). */
  paidAdvances?: Decimal;
  remainingBalance?: Decimal;
  scheduledAt?: IsoDateTime | null;
  completedAt?: IsoDateTime | null;
  createdAt?: IsoDateTime | null;
  updatedAt?: IsoDateTime | null;
  customerName?: string | null;
  vehicleDescription?: string | null;
  items: ServiceOrderItemDto[];
  materials: ServiceMaterialDto[];
  attachments: ServiceAttachmentDto[];
  statusHistory: ServiceStatusHistoryDto[];
  advances: ServiceAdvanceDto[];
}

/** Resumen de orden de servicio para listados. */
export interface ServiceOrderSummaryDto {
  id: Uuid;
  orderNumber: string;
  customerId?: Uuid | null;
  customerName?: string | null;
  vehicleId?: Uuid | null;
  serviceType: ServiceTypeCode | string;
  description: string;
  status: string;
  assignedUserId?: Uuid | null;
  laborAmount: Decimal;
  estimatedTotal: Decimal;
  remainingBalance?: Decimal;
  scheduledAt?: IsoDateTime | null;
  createdAt?: IsoDateTime | null;
}

/** Solicitud de creación de orden de servicio. */
export interface CreateServiceOrderRequest {
  customerId: Uuid;
  vehicleId?: Uuid | null;
  serviceType: ServiceTypeCode | string;
  description: string;
  dimensions?: string | null;
  scheduledAt?: IsoDateTime | null;
  laborAmount: Decimal;
  assignedUserId?: Uuid | null;
  confirmed?: boolean;
  items: ServiceOrderItemRequest[];
  materials: ServiceMaterialRequest[];
  attachmentIds?: Uuid[];
}

/** Solicitud de actualización de orden de servicio. */
export interface UpdateServiceOrderRequest {
  customerId?: Uuid;
  vehicleId?: Uuid | null;
  serviceType?: ServiceTypeCode | string;
  description?: string;
  dimensions?: string | null;
  scheduledAt?: IsoDateTime | null;
  laborAmount?: Decimal;
  assignedUserId?: Uuid | null;
}

/** Solicitud de cambio de estado. */
export interface ChangeServiceStatusRequest {
  status: string;
  notes?: string | null;
}

/** Solicitud de consumo de material. */
export interface ConsumeMaterialRequest {
  quantity: Decimal;
  notes?: string | null;
}

/** Pago de cobro de orden. */
export interface ChargePaymentItem {
  method: PaymentMethodCode | string;
  amount: Decimal;
  reference?: string | null;
}

/** Solicitud de cobro de saldo de orden de servicio. */
export interface ChargeRequest {
  cashSessionId: Uuid;
  payments: ChargePaymentItem[];
}

/** Agenda diaria de órdenes de servicio. */
export interface AgendaDayDto {
  date: string;
  orders: ServiceOrderSummaryDto[];
}
