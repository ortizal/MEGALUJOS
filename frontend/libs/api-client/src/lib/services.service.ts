import { Injectable } from '@angular/core';
import {
  AgendaDayDto,
  ChargeRequest,
  ChangeServiceStatusRequest,
  ConsumeMaterialRequest,
  CreateAdvanceRequest,
  CreateServiceOrderRequest,
  PageResponse,
  SaleResponseDto,
  ServiceAdvanceDto,
  ServiceAttachmentDto,
  ServiceOrderDto,
  ServiceOrderSummaryDto,
  UpdateServiceOrderRequest,
} from '@megalujos/models';
import { Observable, map } from 'rxjs';
import { ApiBaseService, ListParams } from './api-base';

/** Parámetros de listado de órdenes de servicio. */
export interface ServiceOrderListParams extends ListParams {
  status?: string;
  type?: string;
  q?: string;
  assignedUserId?: string;
}

function withPaidAdvances(order: ServiceOrderDto): ServiceOrderDto {
  return { ...order, paidAdvances: order.paidAdvances ?? order.advancesTotal ?? '0' };
}

/** Órdenes de servicio: CRUD, estados, materiales, anticipos, cobros y adjuntos. */
@Injectable({ providedIn: 'root' })
export class ServicesService extends ApiBaseService {
  /** Listado paginado de órdenes de servicio. */
  list(params?: ServiceOrderListParams): Observable<PageResponse<ServiceOrderSummaryDto>> {
    return this.getPaged<ServiceOrderSummaryDto>('/service-orders', params);
  }

  /** Detalle completo de una orden (items, materiales, adjuntos, historial, anticipos). */
  get(id: string): Observable<ServiceOrderDto> {
    return this.httpGet<ServiceOrderDto>(`/service-orders/${id}`).pipe(map(withPaidAdvances));
  }

  /** Alias de `get`. */
  getById(id: string): Observable<ServiceOrderDto> {
    return this.get(id);
  }

  /** Crea una orden de servicio. */
  create(request: CreateServiceOrderRequest): Observable<ServiceOrderDto> {
    return this.httpPost<ServiceOrderDto>('/service-orders', request).pipe(map(withPaidAdvances));
  }

  /** Actualiza datos de una orden. */
  update(id: string, request: UpdateServiceOrderRequest): Observable<ServiceOrderDto> {
    return this.httpPut<ServiceOrderDto>(`/service-orders/${id}`, request).pipe(map(withPaidAdvances));
  }

  /**
   * Cambia el estado de la orden.
   * @param status nuevo estado (`ServiceStatus`)
   * @param notes notas opcionales del cambio
   */
  updateStatus(id: string, status: string, notes?: string | null): Observable<ServiceOrderDto> {
    const request: ChangeServiceStatusRequest = { status, notes: notes ?? undefined };
    return this.changeStatus(id, request);
  }

  /** Cambia el estado enviando el objeto completo. */
  changeStatus(id: string, request: ChangeServiceStatusRequest): Observable<ServiceOrderDto> {
    return this.httpPost<ServiceOrderDto>(`/service-orders/${id}/status`, request).pipe(
      map(withPaidAdvances),
    );
  }

  /**
   * Registra el consumo real de un material (descuenta stock trazable).
   * @param quantity cantidad consumida (formato decimal)
   */
  consumeMaterial(
    id: string,
    materialId: string,
    quantity: string | number,
    notes?: string | null,
  ): Observable<ServiceOrderDto> {
    const request: ConsumeMaterialRequest = { quantity: String(quantity), notes: notes ?? undefined };
    return this.httpPost<ServiceOrderDto>(
      `/service-orders/${id}/materials/${materialId}/consume`,
      request,
    ).pipe(map(withPaidAdvances));
  }

  /** Devuelve material consumido (movimiento compensatorio). */
  returnMaterial(
    id: string,
    materialId: string,
    quantity: string | number,
    notes?: string | null,
  ): Observable<ServiceOrderDto> {
    const request: ConsumeMaterialRequest = { quantity: String(quantity), notes: notes ?? undefined };
    return this.httpPost<ServiceOrderDto>(
      `/service-orders/${id}/materials/${materialId}/return`,
      request,
    ).pipe(map(withPaidAdvances));
  }

  /** Anticipos registrados de la orden. */
  listAdvances(id: string): Observable<ServiceAdvanceDto[]> {
    return this.httpGet<ServiceAdvanceDto[]>(`/service-orders/${id}/advances`);
  }

  /** Registra un anticipo sobre la orden. */
  addAdvance(id: string, request: CreateAdvanceRequest): Observable<ServiceAdvanceDto> {
    return this.httpPost<ServiceAdvanceDto>(`/service-orders/${id}/advances`, request);
  }

  /**
   * Cobra el saldo de la orden generando una venta (pago u otra forma de pago).
   * @param idempotencyKey clave única (UUID) opcional para reintentos seguros.
   */
  charge(id: string, request: ChargeRequest, idempotencyKey?: string): Observable<SaleResponseDto> {
    return this.httpPost<SaleResponseDto>(`/service-orders/${id}/charge`, request, {
      headers: this.idempotencyHeaders(idempotencyKey),
    });
  }

  /** Adjunta un archivo (media ya subido) a la orden. */
  addAttachment(id: string, mediaId: string, type?: string): Observable<ServiceAttachmentDto> {
    return this.httpPost<ServiceAttachmentDto>(`/service-orders/${id}/attachments`, {
      mediaId,
      attachmentType: type,
    });
  }

  /** Elimina un adjunto de la orden. */
  removeAttachment(id: string, mediaId: string): Observable<void> {
    return this.httpDelete<void>(`/service-orders/${id}/attachments/${mediaId}`);
  }

  /** Adjuntos de la orden. */
  listAttachments(id: string): Observable<ServiceAttachmentDto[]> {
    return this.httpGet<ServiceAttachmentDto[]>(`/service-orders/${id}/attachments`);
  }

  /** Imágenes (diseños) adjuntas de la orden. */
  listImages(id: string): Observable<ServiceAttachmentDto[]> {
    return this.httpGet<ServiceAttachmentDto[]>(`/service-orders/${id}/attachments/images`);
  }

  /** Agenda diaria de órdenes programadas. */
  agenda(): Observable<AgendaDayDto[]> {
    return this.httpGet<AgendaDayDto[]>('/services/agenda');
  }

  /** Alias de `agenda`. */
  getAgenda(): Observable<AgendaDayDto[]> {
    return this.agenda();
  }
}
