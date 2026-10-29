import { Injectable } from '@angular/core';
import {
  CashCloseRequest,
  CashCountRequest,
  CashMovementDto,
  CashMovementRequest,
  CashRegisterDto,
  CashReconciliationDto,
  CashSessionDto,
  PageResponse,
} from '@megalujos/models';
import { Observable, map } from 'rxjs';
import { ApiBaseService, ListParams } from './api-base';

function withReconciliationItems(reconciliation: CashReconciliationDto): CashReconciliationDto {
  return { ...reconciliation, items: reconciliation.items ?? reconciliation.lines ?? [] };
}

/** Caja: registros, sesiones, arqueos y movimientos. */
@Injectable({ providedIn: 'root' })
export class CashService extends ApiBaseService {
  /** Cajas físicas configuradas. */
  registers(): Observable<CashRegisterDto[]> {
    return this.httpGet<CashRegisterDto[]>('/cash/registers');
  }

  /** Sesión de caja actualmente abierta (opcionalmente por registro). */
  activeSession(registerId?: string): Observable<CashSessionDto> {
    return this.httpGet<CashSessionDto>('/cash/sessions/active', { registerId });
  }

  /** Abre una sesión de caja con fondo inicial. */
  openSession(request: {
    registerId: string;
    openingAmount: string;
    notes?: string | null;
  }): Observable<CashSessionDto> {
    return this.httpPost<CashSessionDto>('/cash/sessions/open', request);
  }

  /** Cierra una sesión con arqueo final. */
  closeSession(id: string, request: CashCloseRequest): Observable<CashSessionDto> {
    return this.httpPost<CashSessionDto>(`/cash/sessions/${id}/close`, request);
  }

  /** Movimientos de una sesión (cobros, ingresos y retiros). */
  movements(id: string): Observable<CashMovementDto[]> {
    return this.httpGet<CashMovementDto[]>(`/cash/sessions/${id}/movements`);
  }

  /** Registra un ingreso o retiro manual de caja. */
  addMovement(id: string, request: CashMovementRequest): Observable<CashMovementDto> {
    return this.httpPost<CashMovementDto>(`/cash/sessions/${id}/movements`, request);
  }

  /** Historial paginado de sesiones de caja. */
  sessions(params?: ListParams & { status?: string }): Observable<PageResponse<CashSessionDto>> {
    return this.getPaged<CashSessionDto>('/cash/sessions', params);
  }

  /** Arqueo por medio de pago (esperado vs contado y diferencia). */
  reconciliation(id: string): Observable<CashReconciliationDto> {
    return this.httpGet<CashReconciliationDto>(`/cash/sessions/${id}/reconciliation`).pipe(
      map(withReconciliationItems),
    );
  }

  /** Guarda los conteos físicos por medio de pago y devuelve el arqueo. */
  saveCounts(id: string, request: CashCountRequest): Observable<CashReconciliationDto> {
    return this.httpPost<CashReconciliationDto>(`/cash/sessions/${id}/counts`, request).pipe(
      map(withReconciliationItems),
    );
  }
}
