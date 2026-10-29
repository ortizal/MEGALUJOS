import { Injectable } from '@angular/core';
import {
  CreateUserRequest,
  PageResponse,
  UpdateUserRequest,
  UserDto,
} from '@megalujos/models';
import { Observable } from 'rxjs';
import { ApiBaseService, ListParams } from './api-base';

/** Administración de usuarios. */
@Injectable({ providedIn: 'root' })
export class UsersAdminService extends ApiBaseService {
  /** Listado paginado de usuarios. */
  list(params?: ListParams & { q?: string; active?: boolean }): Observable<PageResponse<UserDto>> {
    return this.getPaged<UserDto>('/users', params);
  }

  /** Detalle de usuario por id. */
  getById(id: string): Observable<UserDto> {
    return this.httpGet<UserDto>(`/users/${id}`);
  }

  /** Crea un usuario. */
  create(request: CreateUserRequest): Observable<UserDto> {
    return this.httpPost<UserDto>('/users', request);
  }

  /** Actualiza datos y roles de un usuario. */
  update(id: string, request: UpdateUserRequest): Observable<UserDto> {
    return this.httpPut<UserDto>(`/users/${id}`, request);
  }

  /** Activa/desactiva un usuario. */
  setActive(id: string, active: boolean): Observable<UserDto> {
    return this.httpPatch<UserDto>(`/users/${id}/active`, undefined, { active });
  }

  /** Restablece la contraseña de un usuario (admin). */
  resetPassword(id: string, newPassword: string): Observable<void> {
    return this.httpPost<void>(`/users/${id}/reset-password`, { newPassword });
  }
}
