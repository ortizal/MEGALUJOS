import { isPlatformBrowser } from '@angular/common';
import { computed, inject, Injectable, PLATFORM_ID, signal, WritableSignal } from '@angular/core';
import { AuthService as AuthApiService } from '@megalujos/api-client';
import {
  AuthUser,
  ChangePasswordRequest,
  LoginRequest,
  LoginResponse,
  UserDto,
} from '@megalujos/models';
import { Observable, finalize, map, shareReplay, tap } from 'rxjs';

/** Clave de persistencia de la sesión local. */
export const MEGA_AUTH_STORAGE_KEY = 'mega.auth';

/** Sesión persistida. */
interface PersistedSession {
  accessToken: string;
  refreshToken: string;
  user: AuthUser | null;
}

/** Convierte el `UserDto` de `/auth/me` en `AuthUser` (`userId` = `id`). */
function toAuthUser(dto: UserDto): AuthUser {
  return {
    userId: dto.id,
    id: dto.id,
    username: dto.username,
    fullName: dto.fullName,
    roles: dto.roles ?? [],
    permissions: dto.permissions ?? [],
  };
}

/**
 * Estado de autenticación con signals.
 * - Access token + refresh token + usuario en localStorage (persistencia entre sesiones).
 * - `refreshSession()` serializa reintentos concurrentes con un solo refresh (cola simple).
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(AuthApiService);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly isBrowser = isPlatformBrowser(this.platformId);

  /** JWT de acceso actual. */
  readonly token = signal<string | null>(this.restore()?.accessToken ?? null);

  /** Alias del contrato (`token` es la señal canónica). */
  readonly accessToken: WritableSignal<string | null> = this.token;

  /** Refresh token actual. */
  readonly refreshToken = signal<string | null>(this.restore()?.refreshToken ?? null);

  /** Usuario autenticado (id, nombre, roles, permisos). */
  readonly user = signal<AuthUser | null>(this.restore()?.user ?? null);

  /** true cuando existe sesión completa. */
  readonly isLoggedIn = computed(() => !!this.accessToken() && !!this.user());

  /** Roles del usuario actual. */
  readonly roles = computed<string[]>(() => this.user()?.roles ?? []);

  /** Permisos del usuario actual. */
  readonly permissions = computed<string[]>(() => this.user()?.permissions ?? []);

  private refreshInFlight$: Observable<LoginResponse> | null = null;
  private sessionCache: PersistedSession | null | undefined;

  // ------------------------------------------------------------------- sesión

  /** Inicia sesión y persiste la sesión completa. */
  login(request: LoginRequest): Observable<LoginResponse> {
    return this.api.login(request).pipe(tap((response) => this.applySession(response)));
  }

  /**
   * Renovación de tokens con cola simple: varias peticiones 401 concurrentes
   * comparten una única llamada a /auth/refresh.
   */
  refreshSession(): Observable<LoginResponse> {
    const currentRefresh = this.refreshToken();
    if (!currentRefresh) {
      throw new Error('No hay refresh token disponible');
    }
    if (!this.refreshInFlight$) {
      this.refreshInFlight$ = this.api.refresh({ refreshToken: currentRefresh }).pipe(
        tap((response) => this.applySession(response)),
        finalize(() => {
          this.refreshInFlight$ = null;
        }),
        shareReplay({ bufferSize: 1, refCount: false }),
      );
    }
    return this.refreshInFlight$;
  }

  /** Usuario actual según el JWT (refresca los permisos en caliente). */
  loadMe(): Observable<AuthUser> {
    return this.api.me().pipe(
      map((dto) => toAuthUser(dto)),
      tap((user) => {
        this.user.set(user);
        this.persist();
      }),
    );
  }

  /** Cambio de contraseña del usuario actual. */
  changePassword(request: ChangePasswordRequest): Observable<void> {
    return this.api.changePassword(request);
  }

  /** Cierra la sesión y limpia el almacenamiento. */
  logout(): void {
    this.accessToken.set(null);
    this.refreshToken.set(null);
    this.user.set(null);
    this.sessionCache = null;
    if (this.isBrowser) {
      try {
        localStorage.removeItem(MEGA_AUTH_STORAGE_KEY);
      } catch {
        /* almacenamiento no disponible */
      }
    }
  }

  // -------------------------------------------------------------- autorización

  /** true si el usuario tiene el permiso indicado (o es ADMIN implícito del backend). */
  hasPermission(permission: string): boolean {
    return this.permissions().includes(permission);
  }

  /** true si el usuario tiene al menos uno de los permisos indicados. */
  hasAnyPermission(permissions: string[]): boolean {
    if (permissions.length === 0) {
      return true;
    }
    const current = new Set(this.permissions());
    return permissions.some((permission) => current.has(permission));
  }

  /** true si el usuario posee TODOS los permisos indicados. */
  hasAllPermissions(permissions: string[]): boolean {
    if (permissions.length === 0) {
      return true;
    }
    const current = new Set(this.permissions());
    return permissions.every((permission) => current.has(permission));
  }

  /** true si el usuario tiene alguno de los roles indicados. */
  hasRole(...roles: string[]): boolean {
    const current = new Set(this.roles());
    return roles.some((role) => current.has(role));
  }

  // ------------------------------------------------------------------ internos

  /** ¿La URL corresponde a un endpoint de autenticación (evita bucles de refresh)? */
  isAuthUrl(url: string): boolean {
    return url.includes('/auth/login') || url.includes('/auth/refresh');
  }

  private applySession(response: LoginResponse): void {
    this.token.set(response.accessToken ?? null);
    this.refreshToken.set(response.refreshToken ?? null);
    this.user.set({
      userId: response.userId,
      id: response.userId,
      username: response.username,
      fullName: response.fullName,
      roles: response.roles ?? [],
      permissions: response.permissions ?? [],
    });
    this.persist();
  }

  private persist(): void {
    if (!this.isBrowser) {
      return;
    }
    const session: PersistedSession = {
      accessToken: this.accessToken() ?? '',
      refreshToken: this.refreshToken() ?? '',
      user: this.user(),
    };
    try {
      localStorage.setItem(MEGA_AUTH_STORAGE_KEY, JSON.stringify(session));
    } catch {
      /* almacenamiento no disponible */
    }
  }

  private restore(): PersistedSession | null {
    if (this.sessionCache !== undefined) {
      return this.sessionCache;
    }
    if (!this.isBrowser) {
      this.sessionCache = null;
      return null;
    }
    try {
      const raw = localStorage.getItem(MEGA_AUTH_STORAGE_KEY);
      this.sessionCache = raw ? (JSON.parse(raw) as PersistedSession) : null;
    } catch {
      this.sessionCache = null;
    }
    return this.sessionCache;
  }
}
