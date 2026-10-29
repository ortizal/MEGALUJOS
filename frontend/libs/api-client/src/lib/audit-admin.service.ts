import { Injectable } from '@angular/core';
import { AuditLogDto, PageResponse } from '@megalujos/models';
import { Observable } from 'rxjs';
import { ApiBaseService, ListParams } from './api-base';

/** Consulta de la traza de auditoría. */
@Injectable({ providedIn: 'root' })
export class AuditAdminService extends ApiBaseService {
  /** Búsqueda paginada de eventos de auditoría. */
  list(
    params?: ListParams & { actorId?: string; entityType?: string; action?: string },
  ): Observable<PageResponse<AuditLogDto>> {
    return this.getPaged<AuditLogDto>('/audit', params);
  }
}
