import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/**
 * Guard funcional de sesión: redirige a /login conservando la URL destino.
 * Es una barrera de UX; la autorización real siempre la valida el backend.
 */
export const authGuard: CanActivateFn = (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isLoggedIn()) {
    return true;
  }
  return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};

/**
 * Guard funcional de permisos: requiere TODOS los permisos indicados
 * (principio de mínimo privilegio) y redirige a /acceso-denegado.
 *
 * Uso en rutas:
 * ```ts
 * { path: 'reportes', canActivate: [permissionGuard(['reports:view'])], ... }
 * ```
 */
export function permissionGuard(permissions: string[]): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    if (auth.isLoggedIn() && (auth.hasRole('ADMIN') || auth.hasAllPermissions(permissions))) {
      return true;
    }
    if (!auth.isLoggedIn()) {
      return router.createUrlTree(['/login']);
    }
    return router.createUrlTree(['/acceso-denegado']);
  };
}

/** Guard funcional que exige tener sesión abierta para ver /login (evita bucle). */
export const anonymousGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.isLoggedIn() ? router.createUrlTree(['/']) : true;
};
