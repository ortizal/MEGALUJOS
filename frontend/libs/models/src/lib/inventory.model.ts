import { Decimal, IsoDateTime, Uuid } from './common.model';

/** Saldo de stock por variante y almacén. */
export interface StockBalanceDto {
  variantId: Uuid;
  warehouseId: Uuid;
  sku: string;
  productName: string;
  warehouseName?: string | null;
  quantity: Decimal;
  reserved?: Decimal;
  available: Decimal;
  minStock?: Decimal | null;
  /** true cuando `available <= minStock` (lo normaliza InventoryService). */
  lowStock: boolean;
}

/** Movimiento inmutable de kardex. */
export interface StockMovementDto {
  id: Uuid;
  variantId: Uuid;
  sku?: string | null;
  productName?: string | null;
  warehouseId?: Uuid | null;
  warehouseName?: string | null;
  movementType: string;
  quantityDelta: Decimal;
  balanceAfter?: Decimal | null;
  unitCost?: Decimal | null;
  sourceType: string;
  sourceId?: Uuid | null;
  notes?: string | null;
  createdAt: IsoDateTime;
}

/** Alerta de stock mínimo. */
export interface StockAlertDto {
  variantId: Uuid;
  sku: string;
  productName: string;
  quantity: Decimal;
  minStock: Decimal;
  warehouseName?: string | null;
  lowStock?: boolean;
}

/** Ítem de ajuste solicitado. */
export interface AdjustmentItemRequest {
  variantId: Uuid;
  adjustedQuantity: Decimal;
  cost?: Decimal | null;
}

/** Solicitud de ajuste de inventario. */
export interface AdjustmentRequest {
  warehouseId?: Uuid | null;
  reason: string;
  items: AdjustmentItemRequest[];
}

/** Nombre del contrato maestro para crear ajustes. */
export type CreateAdjustmentRequest = AdjustmentRequest;

/** Ítem de ajuste (respuesta). */
export interface AdjustmentItemDto {
  id: Uuid;
  variantId: Uuid;
  currentQuantity?: Decimal | null;
  adjustedQuantity: Decimal;
  difference?: Decimal | null;
  cost?: Decimal | null;
}

/** Ajuste de inventario (respuesta). */
export interface AdjustmentDto {
  id: Uuid;
  adjustmentNumber: string;
  warehouseId: Uuid;
  status: string;
  reason?: string | null;
  createdBy?: Uuid | null;
  approvedBy?: Uuid | null;
  createdAt: IsoDateTime;
  approvedAt?: IsoDateTime | null;
  items: AdjustmentItemDto[];
}

/** Ítem de conteo físico. */
export interface StockCountItemRequest {
  variantId: Uuid;
  countedQuantity: Decimal;
}

/** Solicitud de conteo físico. */
export interface StockCountRequest {
  warehouseId?: Uuid | null;
  notes?: string | null;
  items: StockCountItemRequest[];
}

/** Ítem de conteo (respuesta). */
export interface StockCountItemDto {
  id: Uuid;
  variantId: Uuid;
  systemQuantity?: Decimal | null;
  countedQuantity: Decimal;
  difference?: Decimal | null;
}

/** Conteo físico (respuesta). */
export interface StockCountDto {
  id: Uuid;
  countNumber: string;
  warehouseId: Uuid;
  status: string;
  notes?: string | null;
  createdBy?: Uuid | null;
  approvedBy?: Uuid | null;
  createdAt: IsoDateTime;
  completedAt?: IsoDateTime | null;
  approvedAt?: IsoDateTime | null;
  items: StockCountItemDto[];
}
