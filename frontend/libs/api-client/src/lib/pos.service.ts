import { Injectable } from '@angular/core';
import {
  ConfirmSaleRequest,
  ConfirmSaleItemRequest,
  PageResponse,
  SaleDetailDto,
  SaleResponseDto,
  SaleReturnDto,
  SaleReturnRequest,
  SaleSummaryDto,
} from '@megalujos/models';
import { Observable, map } from 'rxjs';
import { ApiBaseService, ListParams } from './api-base';

/** Parámetros de listado de ventas. */
export interface SaleListParams extends ListParams {
  q?: string;
  status?: string;
  customerId?: string;
  cashSessionId?: string;
  dateFrom?: string;
  dateTo?: string;
}

function toWireItem(item: ConfirmSaleItemRequest): ConfirmSaleItemRequest {
  const { discount, discountAmount, ...rest } = item;
  return { ...rest, discount: discount ?? discountAmount ?? '0' };
}

function withSummaryId(summary: SaleSummaryDto): SaleSummaryDto {
  return { ...summary, id: summary.id ?? summary.saleId };
}

/**
 * Punto de venta: confirmación idempotente de ventas, historial y devoluciones.
 * `confirmSale` envía la cabecera `Idempotency-Key` obligatoria del contrato.
 */
@Injectable({ providedIn: 'root' })
export class PosService extends ApiBaseService {
  /**
   * Confirma una venta de forma atómica (bloquea y descuenta stock en servidor).
   * @param idempotencyKey clave única (UUID) reutilizable en reintentos.
   */
  confirmSale(request: ConfirmSaleRequest, idempotencyKey: string): Observable<SaleResponseDto> {
    const body: ConfirmSaleRequest = { ...request, items: request.items.map(toWireItem) };
    return this.httpPost<SaleResponseDto>('/sales', body, {
      headers: this.idempotencyHeaders(idempotencyKey),
    });
  }

  /** Historial paginado de ventas con filtros. */
  listSales(params?: SaleListParams): Observable<PageResponse<SaleSummaryDto>> {
    return this.getPaged<SaleSummaryDto>('/sales', params).pipe(
      map((page) => ({ ...page, items: page.items.map(withSummaryId) })),
    );
  }

  /** Detalle de venta (ítems, impuestos, pagos y devoluciones). */
  getSale(id: string): Observable<SaleDetailDto> {
    return this.httpGet<SaleDetailDto>(`/sales/${id}`);
  }

  /** Registra una devolución. */
  createReturn(saleId: string, request: SaleReturnRequest, idempotencyKey?: string): Observable<SaleReturnDto> {
    return this.httpPost<SaleReturnDto>(`/sales/${saleId}/returns`, request, {
      headers: this.idempotencyHeaders(idempotencyKey),
    });
  }

  /** Devoluciones registradas de una venta. */
  listReturns(saleId: string): Observable<SaleReturnDto[]> {
    return this.httpGet<SaleReturnDto[]>(`/sales/${saleId}/returns`);
  }

  /** Anula una venta completa (solo ADMIN). */
  voidSale(saleId: string, reason?: string): Observable<SaleDetailDto> {
    return this.httpPost<SaleDetailDto>(`/sales/${saleId}/void`, { reason });
  }
}
