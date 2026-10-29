import { InjectionToken, Provider } from '@angular/core';
import { API_URL } from './core/api-url';

/** Configuración inyectable del cliente API. */
export interface ApiConfig {
  /** Prefijo base de la API. Por defecto `/api/v1`. */
  baseUrl: string;
}

/** Valor por defecto del prefijo REST (alias de `API_URL`, contrato: `/api/v1`). */
export const API_BASE_PATH = API_URL;

/** Token de inyección de la configuración API (valor por defecto `/api/v1`). */
export const API_CONFIG = new InjectionToken<ApiConfig>('MEGALUJOS_API_CONFIG', {
  providedIn: 'root',
  factory: () => defaultApiConfig(),
});

/** Configuración por defecto (usa `/api/v1` relativo al mismo origen). */
export function defaultApiConfig(): ApiConfig {
  return { baseUrl: API_BASE_PATH };
}

/** Helper: construye la URL absoluta para un path relativo al prefijo. */
export function apiUrl(path: string, config: ApiConfig = defaultApiConfig()): string {
  const base = config.baseUrl.replace(/\/+$/, '');
  const suffix = path.startsWith('/') ? path : `/${path}`;
  return `${base}${suffix}`;
}

/** Providers para sobreescribir la configuración (p.ej. con un proxy en dev). */
export function provideApiConfig(config: Partial<ApiConfig>): Provider[] {
  return [{ provide: API_CONFIG, useValue: { ...defaultApiConfig(), ...config } }];
}
