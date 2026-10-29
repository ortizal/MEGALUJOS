import { Injectable } from '@angular/core';
import { LoginRequest, LoginResponse, RefreshTokenRequest, UserDto } from '@megalujos/models';
import { Observable } from 'rxjs';
import { ApiBaseService } from './api-base';

/** Cabecera que indica al interceptor no intentar refresh (bucle de seguridad). */
export const SKIP_REFRESH_HEADER = 'X-Skip-Refresh';

/** Servicio de autenticación (HTTP puro: login/refresh/me/change-password). */
@Injectable({ providedIn: 'root' })
export class AuthService extends ApiBaseService {
  /** Inicio de sesión. */
  login(request: LoginRequest): Observable<LoginResponse> {
    return this.httpPost<LoginResponse>('/auth/login', request, {
      headers: { [SKIP_REFRESH_HEADER]: 'true' },
    });
  }

  /** Renovación de tokens (una sola petición concurrente vía interceptor). */
  refresh(request: RefreshTokenRequest): Observable<LoginResponse> {
    return this.httpPost<LoginResponse>('/auth/refresh', request, {
      headers: { [SKIP_REFRESH_HEADER]: 'true' },
    });
  }

  /** Usuario autenticado a partir del JWT actual. */
  me(): Observable<UserDto> {
    return this.httpGet<UserDto>('/auth/me');
  }

  /** Cambio de contraseña del usuario actual. */
  changePassword(body: { currentPassword: string; newPassword: string }): Observable<void> {
    return this.httpPost<void>('/auth/change-password', body);
  }
}
