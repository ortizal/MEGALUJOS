import { HttpErrorResponse, HttpInterceptorFn, provideHttpClient, withInterceptors } from '@angular/common/http';
import { isPlatformBrowser } from '@angular/common';
import { inject, EnvironmentProviders, PLATFORM_ID } from '@angular/core';
import { Router } from '@angular/router';
import { SKIP_REFRESH_HEADER } from '@megalujos/api-client';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';

/**
 * Interceptor funcional de autenticación:
 * 1. Añade `Authorization: Bearer <token>` a las peticiones de la API.
 * 2. Ante un 401 (y sin refresh en curso) intenta UN refresh con cola simple
 *    y reintenta la petición original una vez.
 * 3. Si el refresh falla → logout y recarga hacia /login.
 * 4. Las peticiones marcadas con `X-Skip-Refresh` o a `/auth/login|refresh` se excluyen.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const platformId = inject(PLATFORM_ID);

  const isAuthEndpoint = auth.isAuthUrl(req.url);
  const skipRefresh = req.headers.has(SKIP_REFRESH_HEADER);
  const token = auth.token();

  const authorized = req
    .clone({ headers: req.headers.delete(SKIP_REFRESH_HEADER) });
  const withToken =
    token && !authorized.headers.has('Authorization')
      ? authorized.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : authorized;

  return next(withToken).pipe(
    catchError((error: unknown) => {
      const isUnauthorized = error instanceof HttpErrorResponse && error.status === 401;
      if (!isUnauthorized || isAuthEndpoint || skipRefresh || !auth.refreshToken()) {
        return throwError(() => error);
      }

      return auth.refreshSession().pipe(
        switchMap(() =>
          next(
            req.clone({
              headers: req.headers.delete(SKIP_REFRESH_HEADER),
              setHeaders: { Authorization: `Bearer ${auth.token() ?? ''}` },
            }),
          ),
        ),
        catchError((refreshError: unknown) => {
          auth.logout();
          if (isPlatformBrowser(platformId)) {
            window.location.href = '/login';
          } else {
            void router.navigate(['/login']);
          }
          return throwError(() => refreshError);
        }),
      );
    }),
  );
};

/** Lista funcional de interceptores de la librería (útil con `withInterceptors`). */
export const authInterceptors: HttpInterceptorFn[] = [authInterceptor];

/**
 * Registra el interceptor de autenticación en la aplicación:
 * `provideHttpClient(withInterceptors(authInterceptors))`.
 */
export function provideAuthInterceptor(): EnvironmentProviders {
  return provideHttpClient(withInterceptors(authInterceptors));
}

