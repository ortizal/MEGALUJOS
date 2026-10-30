/** Configuración de desarrollo del panel admin (API relativa vía proxy). */
export const environment = {
  production: false,
  /** Prefijo de la API. En dev el proxy de Angular reenvía a localhost:8080. */
  apiBaseUrl: '/api/v1',
};
