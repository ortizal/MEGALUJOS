/** Respuesta paginada uniforme (contrato backend PageResponse). */
export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/** Parámetros de paginación aceptados por los endpoints. */
export interface PageRequest {
  page?: number;
  size?: number;
  sort?: string;
}

/** Errores problem+json (type, title, status, detail, instance, traceId, fieldErrors). */
export interface ApiProblem {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  traceId?: string;
  fieldErrors?: Record<string, string[]>;
}

/** Nombre RFC-7807 del problema (alias del contrato maestro). */
export type ProblemDetails = ApiProblem;

export type Decimal = string;
export type IsoDateTime = string;
export type IsoDate = string;
export type Uuid = string;
