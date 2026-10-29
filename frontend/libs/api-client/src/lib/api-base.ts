import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { PageResponse } from '@megalujos/models';
import { Observable, OperatorFunction, catchError, map, throwError } from 'rxjs';
import { ApiConfig, API_CONFIG, apiUrl } from './api-config';
import { ApiError, toApiError } from './util/api-error';

/** Opciones comunes de listado paginado. */
export interface ListParams {
  page?: number;
  size?: number;
  sort?: string;
  [key: string]: string | number | boolean | undefined | null;
}

/**
 * Normaliza una respuesta paginada a `PageResponse<T>`.
 * Acepta tanto el `PageResponse` del backend (`items`) como el `Page` de Spring
 * (`content`), usado puntualmente por el catálogo público.
 */
export function normalizePage<T>(raw: unknown): PageResponse<T> {
  const source = (raw ?? {}) as Partial<PageResponse<T>> & {
    content?: T[];
    totalElements?: number;
    totalPages?: number;
    number?: number;
  };
  const items = (source.items ?? source.content ?? []) as T[];
  const size = source.size ?? items.length;
  const totalElements = source.totalElements ?? items.length;
  return {
    items,
    page: source.page ?? source.number ?? 0,
    size,
    totalElements,
    totalPages: source.totalPages ?? (size > 0 ? Math.ceil(totalElements / size) : 0),
  };
}

/**
 * Base de todos los servicios API tipados.
 * - `url()` resuelve contra `ApiConfig.baseUrl` (`/api/v1`).
 * - `params()` construye HttpParams omitiendo valores vacíos.
 * - `handle()` normaliza cualquier error a `ApiError` (problem+json).
 */
@Injectable()
export abstract class ApiBaseService {
  protected readonly http = inject(HttpClient);
  protected readonly apiConfig: ApiConfig = inject(API_CONFIG);

  /** URL completa para un path relativo al prefijo de la API. */
  protected url(path: string): string {
    return apiUrl(path, this.apiConfig);
  }

  /** Construye HttpParams a partir de un objeto de filtros (omite null/undefined/''). */
  protected params(query?: Record<string, string | number | boolean | undefined | null>): HttpParams {
    let params = new HttpParams();
    if (!query) {
      return params;
    }
    for (const [key, value] of Object.entries(query)) {
      if (value === undefined || value === null || value === '') {
        continue;
      }
      params = params.set(key, String(value));
    }
    return params;
  }

  /** Normaliza cualquier error a ApiError. */
  protected handle<T>(): OperatorFunction<T, T> {
    return catchError((error: unknown) => throwError(() => toApiError(error)));
  }

  /** Normaliza error a ApiError sin lanzar (útil para respuestas alternativas). */
  protected toError(error: unknown): ApiError {
    return toApiError(error);
  }

  /**
   * Atajo para peticiones GET tipadas con manejo de errores.
   * (Prefijo `http` para no colisionar con métodos públicos `get()` de servicios concretos.)
   */
  protected httpGet<T>(path: string, query?: Record<string, string | number | boolean | undefined | null>): Observable<T> {
    return this.http.get<T>(this.url(path), { params: this.params(query) }).pipe(this.handle<T>());
  }

  /** Atajo para peticiones POST tipadas con manejo de errores. */
  protected httpPost<T>(path: string, body: unknown, options?: object): Observable<T> {
    return this.http.post<T>(this.url(path), body, options).pipe(this.handle<T>());
  }

  /** Atajo para peticiones PUT tipadas con manejo de errores. */
  protected httpPut<T>(path: string, body: unknown): Observable<T> {
    return this.http.put<T>(this.url(path), body).pipe(this.handle<T>());
  }

  /** Atajo para peticiones PATCH tipadas con manejo de errores. */
  protected httpPatch<T>(path: string, body?: unknown, query?: Record<string, string | number | boolean | undefined | null>): Observable<T> {
    return this.http.patch<T>(this.url(path), body, { params: this.params(query) }).pipe(this.handle<T>());
  }

  /** Atajo para peticiones DELETE tipadas con manejo de errores. */
  protected httpDelete<T>(path: string): Observable<T> {
    return this.http.delete<T>(this.url(path)).pipe(this.handle<T>());
  }

  /** GET paginado que normaliza la respuesta a `PageResponse<T>`. */
  protected getPaged<T>(
    path: string,
    query?: Record<string, string | number | boolean | undefined | null>,
  ): Observable<PageResponse<T>> {
    return this.httpGet<unknown>(path, query).pipe(map((raw) => normalizePage<T>(raw)));
  }

  /** Cabeceras para peticiones idempotentes (`Idempotency-Key`). */
  protected idempotencyHeaders(idempotencyKey?: string | null): HttpHeaders {
    let headers = new HttpHeaders();
    if (idempotencyKey) {
      headers = headers.set('Idempotency-Key', idempotencyKey);
    }
    return headers;
  }
}
