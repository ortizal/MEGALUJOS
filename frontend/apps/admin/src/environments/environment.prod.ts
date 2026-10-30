/**
 * Configuración de producción del panel admin.
 * Bajo `/megalujos/admin/` la API se sirve namespaced: nginx reescribe
 * `/megalujos/api/(.*)` → `/api/v1/$1` hacia el backend en Docker (127.0.0.1:18090).
 */
export const environment = {
  production: true,
  apiBaseUrl: '/megalujos/api',
};
