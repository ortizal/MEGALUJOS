import { Injectable } from '@angular/core';
import { PageResponse, PrintJobDto, PrintJobRequest } from '@megalujos/models';
import { Observable } from 'rxjs';
import { ApiBaseService, ListParams } from './api-base';

/** Cola de trabajos de impresión. */
@Injectable({ providedIn: 'root' })
export class PrintService extends ApiBaseService {
  /** Encola un trabajo de impresión (ticket, cierre, orden, etiqueta). */
  createJob(request: PrintJobRequest): Observable<PrintJobDto> {
    return this.httpPost<PrintJobDto>('/print-jobs', request);
  }

  /** Listado paginado de trabajos de impresión. */
  list(params?: ListParams & { status?: string }): Observable<PageResponse<PrintJobDto>> {
    return this.getPaged<PrintJobDto>('/print-jobs', params);
  }

  /** Alias de `list`. */
  listJobs(params?: ListParams & { status?: string }): Observable<PageResponse<PrintJobDto>> {
    return this.list(params);
  }

  /** Marca un trabajo como impreso (auditoría de reimpresiones). */
  markPrinted(id: string): Observable<PrintJobDto> {
    return this.httpPost<PrintJobDto>(`/print-jobs/${id}/printed`, {});
  }
}
