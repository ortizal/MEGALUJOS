import { Injectable } from '@angular/core';
import {
  AdjustmentDto,
  AdjustmentRequest,
  PageResponse,
  StockAlertDto,
  StockBalanceDto,
  StockCountDto,
  StockCountRequest,
  StockMovementDto,
} from '@megalujos/models';
import { Observable, map } from 'rxjs';
import { ApiBaseService, ListParams } from './api-base';

function withLowStock(balance: StockBalanceDto): StockBalanceDto {
  const min = Number(balance.minStock ?? 0);
  const available = Number(balance.available ?? 0);
  return { ...balance, lowStock: balance.lowStock ?? (min > 0 && available <= min) };
}

/** Inventario: saldos, kardex, ajustes, conteos y alertas. */
@Injectable({ providedIn: 'root' })
export class InventoryService extends ApiBaseService {
  /** Saldos paginados por variante y almacén. */
  balances(
    params?: ListParams & { q?: string; warehouseId?: string; lowStockOnly?: boolean },
  ): Observable<PageResponse<StockBalanceDto>> {
    return this.getPaged<StockBalanceDto>('/inventory/balances', params).pipe(
      map((page) => ({ ...page, items: page.items.map(withLowStock) })),
    );
  }

  /** Kardex paginado de movimientos inmutables. */
  movements(
    params?: ListParams & {
      variantId?: string;
      warehouseId?: string;
      movementType?: string;
      dateFrom?: string;
      dateTo?: string;
    },
  ): Observable<PageResponse<StockMovementDto>> {
    return this.getPaged<StockMovementDto>('/inventory/movements', params);
  }

  /** Crea un ajuste de inventario (borrador). */
  createAdjustment(request: AdjustmentRequest): Observable<AdjustmentDto> {
    return this.httpPost<AdjustmentDto>('/inventory/adjustments', request);
  }

  /** Aprueba un ajuste pendiente (aplica las diferencias al stock). */
  approveAdjustment(id: string): Observable<AdjustmentDto> {
    return this.httpPost<AdjustmentDto>(`/inventory/adjustments/${id}/approve`, {});
  }

  /** Listado paginado de ajustes. */
  listAdjustments(params?: ListParams): Observable<PageResponse<AdjustmentDto>> {
    return this.getPaged<AdjustmentDto>('/inventory/adjustments', params);
  }

  /** Detalle de un ajuste. */
  getAdjustment(id: string): Observable<AdjustmentDto> {
    return this.httpGet<AdjustmentDto>(`/inventory/adjustments/${id}`);
  }

  /** Listado paginado de conteos físicos. */
  listCounts(params?: ListParams): Observable<PageResponse<StockCountDto>> {
    return this.getPaged<StockCountDto>('/inventory/counts', params);
  }

  /** Crea un conteo físico. */
  createCount(request: StockCountRequest): Observable<StockCountDto> {
    return this.httpPost<StockCountDto>('/inventory/counts', request);
  }

  /** Marca un conteo como completado (listo para aprobación). */
  completeCount(id: string): Observable<StockCountDto> {
    return this.httpPost<StockCountDto>(`/inventory/counts/${id}/complete`, {});
  }

  /** Aprueba un conteo y aplica las diferencias. */
  approveCount(id: string): Observable<StockCountDto> {
    return this.httpPost<StockCountDto>(`/inventory/counts/${id}/approve`, {});
  }

  /** Detalle de un conteo. */
  getCount(id: string): Observable<StockCountDto> {
    return this.httpGet<StockCountDto>(`/inventory/counts/${id}`);
  }

  /** Variantes por debajo del stock mínimo. */
  alerts(): Observable<StockAlertDto[]> {
    return this.httpGet<StockAlertDto[]>('/inventory/alerts');
  }
}
