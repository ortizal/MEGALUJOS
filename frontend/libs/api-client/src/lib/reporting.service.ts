import { Injectable } from '@angular/core';
import {
  CashClosingDto,
  DailySalesDto,
  DashboardStatsDto,
  MarginDto,
  PendingDocumentDto,
  PopularProductDto,
  SalesByCategoryDto,
  ServiceStatusCountDto,
  StockAlertDto,
  TaxSummaryDto,
} from '@megalujos/models';
import { Observable, map } from 'rxjs';
import { ApiBaseService, ListParams } from './api-base';

/** Rango de fechas para reportes (fechas ISO date `YYYY-MM-DD`). */
export type ReportRange = ListParams & {
  dateFrom: string;
  dateTo: string;
};

function withDailySalesAliases(day: DailySalesDto): DailySalesDto {
  return { ...day, taxTotal: day.taxTotal ?? day.tax };
}

function withMarginAliases(row: MarginDto): MarginDto {
  return { ...row, margin: row.margin ?? row.estimatedMargin };
}

function withPopularAliases(row: PopularProductDto): PopularProductDto {
  return {
    ...row,
    productName: row.productName ?? row.description,
    totalRevenue: row.totalRevenue ?? row.revenue,
  };
}

/** Reportes y tableros. */
@Injectable({ providedIn: 'root' })
export class ReportingService extends ApiBaseService {
  /** Estadísticas del dashboard (ventas hoy, stock bajo, caja). */
  dashboard(): Observable<DashboardStatsDto> {
    return this.httpGet<DashboardStatsDto>('/reports/dashboard');
  }

  /** Alias de `dashboard`. */
  getDashboard(): Observable<DashboardStatsDto> {
    return this.dashboard();
  }

  /** Ventas por día en un rango. */
  dailySales(range: ReportRange): Observable<DailySalesDto[]> {
    return this.httpGet<DailySalesDto[]>('/reports/daily-sales', range).pipe(
      map((rows) => rows.map(withDailySalesAliases)),
    );
  }

  /** Alias de `dailySales`. */
  getDailySales(range: ReportRange): Observable<DailySalesDto[]> {
    return this.dailySales(range);
  }

  /** Margen bruto estimado por variante en un rango. */
  margin(range: ReportRange): Observable<MarginDto[]> {
    return this.httpGet<MarginDto[]>('/reports/margin', range).pipe(
      map((rows) => rows.map(withMarginAliases)),
    );
  }

  /** Alias de `margin`. */
  getMargin(range: ReportRange): Observable<MarginDto[]> {
    return this.margin(range);
  }

  /** Variantes bajo stock mínimo. */
  stockAlerts(): Observable<StockAlertDto[]> {
    return this.httpGet<StockAlertDto[]>('/reports/stock-alerts');
  }

  /** Alias de `stockAlerts`. */
  getStockAlerts(): Observable<StockAlertDto[]> {
    return this.stockAlerts();
  }

  /** Ventas agrupadas por categoría. */
  salesByCategory(range: ReportRange): Observable<SalesByCategoryDto[]> {
    return this.httpGet<SalesByCategoryDto[]>('/reports/sales-by-category', range);
  }

  /** Alias de `salesByCategory`. */
  getSalesByCategory(range: ReportRange): Observable<SalesByCategoryDto[]> {
    return this.salesByCategory(range);
  }

  /** Resumen de impuestos por código. */
  taxSummary(range: ReportRange): Observable<TaxSummaryDto[]> {
    return this.httpGet<TaxSummaryDto[]>('/reports/tax-summary', range);
  }

  /** Alias de `taxSummary`. */
  getTaxSummary(range: ReportRange): Observable<TaxSummaryDto[]> {
    return this.taxSummary(range);
  }

  /** Comprobantes electrónicos pendientes agrupados por estado. */
  pendingDocuments(): Observable<PendingDocumentDto[]> {
    return this.httpGet<PendingDocumentDto[]>('/reports/pending-documents');
  }

  /** Alias de `pendingDocuments`. */
  getPendingDocuments(): Observable<PendingDocumentDto[]> {
    return this.pendingDocuments();
  }

  /** Órdenes de servicio agrupadas por estado. */
  servicesByStatus(): Observable<ServiceStatusCountDto[]> {
    return this.httpGet<ServiceStatusCountDto[]>('/reports/services-by-status');
  }

  /** Alias de `servicesByStatus`. */
  getServicesByStatus(): Observable<ServiceStatusCountDto[]> {
    return this.servicesByStatus();
  }

  /** Productos más vendidos en un rango. */
  popularProducts(range: ReportRange & { limit?: number }): Observable<PopularProductDto[]> {
    return this.httpGet<PopularProductDto[]>('/reports/popular-products', range).pipe(
      map((rows) => rows.map(withPopularAliases)),
    );
  }

  /** Alias de `popularProducts`. */
  getPopularProducts(range: ReportRange & { limit?: number }): Observable<PopularProductDto[]> {
    return this.popularProducts(range);
  }

  /** Cierres de caja en un rango. */
  cashClosings(range: ReportRange): Observable<CashClosingDto[]> {
    return this.httpGet<CashClosingDto[]>('/reports/cash-closings', range);
  }

  /** Alias de `cashClosings`. */
  getCashClosings(range: ReportRange): Observable<CashClosingDto[]> {
    return this.cashClosings(range);
  }
}
