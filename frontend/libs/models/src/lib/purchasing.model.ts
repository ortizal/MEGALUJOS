import { Decimal, IsoDateTime, Uuid } from './common.model';

/** Proveedor. */
export interface SupplierDto {
  id: Uuid;
  taxId?: string | null;
  name: string;
  contactName?: string | null;
  email?: string | null;
  phone?: string | null;
  address?: string | null;
  active: boolean;
  createdAt?: IsoDateTime | null;
}

/** Solicitud de proveedor. */
export interface SupplierRequest {
  taxId?: string | null;
  name: string;
  contactName?: string | null;
  email?: string | null;
  phone?: string | null;
  address?: string | null;
  active?: boolean;
}

/** Ítem de orden de compra. */
export interface PurchaseOrderItemDto {
  id: Uuid;
  variantId: Uuid;
  sku?: string | null;
  description?: string | null;
  quantity: Decimal;
  unitCost: Decimal;
  lineTotal: Decimal;
  receivedQuantity?: Decimal | null;
}

/** Solicitud de ítem de orden de compra. */
export interface PurchaseOrderItemRequest {
  variantId: Uuid;
  quantity: Decimal;
  unitCost: Decimal;
}

/** Orden de compra. */
export interface PurchaseOrderDto {
  id: Uuid;
  supplierId: Uuid;
  supplierName?: string | null;
  orderNumber: string;
  status: string;
  orderedAt?: IsoDateTime | null;
  createdAt?: IsoDateTime | null;
  subtotal: Decimal;
  taxTotal: Decimal;
  total: Decimal;
  notes?: string | null;
  items: PurchaseOrderItemDto[];
}

/** Solicitud de orden de compra. */
export interface PurchaseOrderRequest {
  supplierId: Uuid;
  notes?: string | null;
  items: PurchaseOrderItemRequest[];
}

/** Ítem de recepción de mercadería. */
export interface GoodsReceiptItemDto {
  id: Uuid;
  orderItemId: Uuid;
  quantity: Decimal;
}

/** Solicitud de recepción de mercadería. */
export interface GoodsReceiptRequest {
  warehouseId?: Uuid | null;
  notes?: string | null;
  items: Array<{ orderItemId: Uuid; quantity: Decimal }>;
}

/** Recepción de mercadería (respuesta). */
export interface GoodsReceiptDto {
  id: Uuid;
  orderId: Uuid;
  warehouseId: Uuid;
  receiptNumber: string;
  receivedAt: IsoDateTime;
  status: string;
  notes?: string | null;
  items: GoodsReceiptItemDto[];
}
