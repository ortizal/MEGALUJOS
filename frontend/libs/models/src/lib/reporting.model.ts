import { Decimal, IsoDateTime, Uuid } from './common.model';

/** Estadísticas del dashboard. */
export interface DashboardStatsDto {
  totalSalesTodayCount: number;
  totalSalesTodayAmount: Decimal;
  lowStockProductsCount: number;
  openCashSessionsCount: number;
  currentCashSessionBalance?: Decimal;
}

/** Ventas por día. */
export interface DailySalesDto {
  date: string;
  count: number;
  subtotal: Decimal;
  /** Impuestos (campo del backend). */
  tax: Decimal;
  /** Alias del contrato maestro (`taxTotal`); lo normaliza ReportingService. */
  taxTotal: Decimal;
  total: Decimal;
}

/** Margen bruto estimado por variante. */
export interface MarginDto {
  variantId: Uuid;
  sku?: string;
  productName: string;
  quantity: Decimal;
  revenue: Decimal;
  cost: Decimal;
  /** Margen (alias del contrato maestro). */
  margin: Decimal;
  /** Margen (nombre del backend). */
  estimatedMargin: Decimal;
}

/** Resumen de impuestos. */
export interface TaxSummaryDto {
  taxCode: string;
  count?: number;
  taxableAmount: Decimal;
  taxAmount: Decimal;
}

/** Producto popular. */
export interface PopularProductDto {
  variantId?: Uuid;
  /** Nombre del producto (alias del contrato maestro). */
  productName: string;
  quantitySold: Decimal;
  /** Ingresos totales (alias del contrato maestro). */
  totalRevenue: Decimal;
  /** Descripción (campo del backend). */
  description: string;
  /** Ingresos (campo del backend). */
  revenue: Decimal;
}

/** Ventas por categoría. */
export interface SalesByCategoryDto {
  category: string;
  count: number;
  total: Decimal;
}

/** Comprobantes pendientes agrupados por estado. */
export interface PendingDocumentDto {
  status: string;
  count: number;
}

/** Órdenes de servicio agrupadas por estado. */
export interface ServiceStatusCountDto {
  status: string;
  count: number;
  total: Decimal;
}

/** Cierre de caja reportado. */
export interface CashClosingDto {
  sessionId: Uuid;
  registerCode?: string | null;
  openedAt?: IsoDateTime | null;
  closedAt?: IsoDateTime | null;
  openingAmount: Decimal;
  expectedAmount: Decimal;
  countedAmount?: Decimal | null;
  difference?: Decimal | null;
}

/** Trabajo de impresión. */
export interface PrintJobDto {
  id: Uuid;
  jobType: string;
  sourceType?: string | null;
  sourceId?: Uuid | null;
  format: string;
  status: string;
  copies: number;
  printerName?: string | null;
  payload?: Record<string, unknown>;
  createdAt?: IsoDateTime | null;
  printedAt?: IsoDateTime | null;
}

/** Solicitud de trabajo de impresión. */
export interface PrintJobRequest {
  jobType: string;
  sourceType?: string | null;
  sourceId?: Uuid | null;
  format?: string;
  copies?: number;
  printerName?: string | null;
  payload?: Record<string, unknown>;
}

/** Metadatos de archivo subido (MediaService). */
export interface MediaDto {
  id: Uuid;
  url?: string | null;
  storageKey?: string | null;
  originalName?: string | null;
  contentType?: string | null;
  sizeBytes?: number | null;
  publicUrl?: string | null;
}
