import { HttpErrorResponse } from '@angular/common/http';
import type { ApiProblem } from '@megalujos/models';

export type { ApiProblem };

/**
 * Error tipado del cliente API.
 * Envuelve el `application/problem+json` devuelto por el backend y
 * normaliza los errores de red en un ApiProblem accesible.
 */
export class ApiError extends Error {
  readonly problem: ApiProblem;
  readonly status: number;
  readonly traceId?: string;
  readonly fieldErrors?: Record<string, string[]>;

  constructor(problem: ApiProblem, status?: number) {
    super(problem.detail || problem.title || 'Error inesperado del servidor');
    this.name = 'ApiError';
    this.problem = problem;
    this.status = status ?? problem.status ?? 0;
    this.traceId = problem.traceId;
    this.fieldErrors = problem.fieldErrors;
  }

  /** true si el backend devolvió un error de validación por campo. */
  get isValidation(): boolean {
    return !!this.fieldErrors && Object.keys(this.fieldErrors).length > 0;
  }

  /** true si no hubo respuesta del servidor (sin conexión / timeout). */
  get isOffline(): boolean {
    return this.status === 0;
  }
}

/** Type guard para ApiError. */
export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError;
}

function isProblemLike(value: unknown): value is ApiProblem {
  return (
    typeof value === 'object' &&
    value !== null &&
    ('title' in value || 'detail' in value || 'type' in value || 'fieldErrors' in value)
  );
}

/** Convierte un HttpErrorResponse (u otro error) en ApiError. */
export function toApiError(error: unknown): ApiError {
  if (isApiError(error)) {
    return error;
  }
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return new ApiError(
        {
          type: 'about:blank',
          title: 'Sin conexión',
          status: 0,
          detail:
            'No se pudo contactar con el servidor. Verifique su conexión e intente nuevamente.',
        },
        0,
      );
    }
    if (isProblemLike(error.error)) {
      return new ApiError(error.error, error.status);
    }
    if (error.status === 401) {
      return new ApiError(
        { type: 'about:blank', title: 'No autorizado', status: 401, detail: 'Sesión inválida o expirada.' },
        401,
      );
    }
    if (error.status === 403) {
      return new ApiError(
        {
          type: 'about:blank',
          title: 'Acceso denegado',
          status: 403,
          detail: 'No tiene permisos para realizar esta acción.',
        },
        403,
      );
    }
    if (error.status === 404) {
      return new ApiError(
        { type: 'about:blank', title: 'No encontrado', status: 404, detail: 'El recurso solicitado no existe.' },
        404,
      );
    }
    return new ApiError(
      {
        type: 'about:blank',
        title: error.statusText || 'Error HTTP',
        status: error.status,
        detail: typeof error.error === 'string' && error.error ? error.error : error.message,
      },
      error.status,
    );
  }
  const message = error instanceof Error ? error.message : String(error);
  return new ApiError({ type: 'about:blank', title: 'Error inesperado', detail: message });
}

/**
 * Mensaje legible en español para mostrar al usuario.
 * - ApiError → detalle/título del problem+json (con errores por campo si existen).
 * - Error de red/sin conexión → mensaje amable.
 * - Cualquier otro error → mensaje genérico.
 */
export function getErrorMessage(error: unknown): string {
  const apiError = isApiError(error) ? error : toApiError(error);

  if (apiError.isOffline) {
    return 'Sin conexión con el servidor. Verifique su red e intente de nuevo.';
  }

  if (apiError.isValidation && apiError.fieldErrors) {
    const lines = Object.entries(apiError.fieldErrors).map(
      ([field, messages]) => `${field}: ${messages.join(', ')}`,
    );
    return [apiError.problem.detail || apiError.problem.title || 'Revisar los campos marcados.', ...lines].join(
      ' | ',
    );
  }

  if (apiError.status === 403) {
    return apiError.problem.detail || 'No tiene permisos para realizar esta acción.';
  }

  if (apiError.status === 401) {
    return apiError.problem.detail || 'Su sesión expiró. Inicie sesión nuevamente.';
  }

  return (
    apiError.problem.detail ||
    apiError.problem.title ||
    (apiError.status >= 500
      ? 'El servidor tuvo un problema. Intente de nuevo en unos segundos.'
      : 'No se pudo completar la operación.')
  );
}
