import { Injectable } from '@angular/core';
import { PermissionDto, RoleDto } from '@megalujos/models';
import { Observable } from 'rxjs';
import { ApiBaseService } from './api-base';

/** Catálogo de roles y permisos. */
@Injectable({ providedIn: 'root' })
export class RolesAdminService extends ApiBaseService {
  /** Lista todos los roles con sus permisos. */
  list(): Observable<RoleDto[]> {
    return this.httpGet<RoleDto[]>('/roles');
  }

  /** Lista todos los permisos disponibles en el sistema. */
  permissions(): Observable<PermissionDto[]> {
    return this.httpGet<PermissionDto[]>('/roles/permissions');
  }
}
