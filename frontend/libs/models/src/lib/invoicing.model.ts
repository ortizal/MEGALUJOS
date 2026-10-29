import { Decimal, IsoDateTime, Uuid } from './common.model';

/** Emisor tributario. */
export interface IssuerDto {
  id: Uuid;
  branchId?: Uuid | null;
  taxId: string;
  legalName: string;
  tradeName?: string | null;
  establishmentCode: string;
  emissionPointCode: string;
  environment: string;
  mainAddress?: string | null;
  active: boolean;
}

/** Solicitud de actualización de emisor. */
export interface IssuerUpdateRequest {
  taxId: string;
  legalName: string;
  tradeName?: string | null;
  establishmentCode: string;
  emissionPointCode: string;
  environment: string;
  mainAddress?: string | null;
}

/** Evento del proceso de un documento electrónico. */
export interface DocumentEventDto {
  id: Uuid;
  stage?: string | null;
  status?: string | null;
  responseCode?: string | null;
  occurredAt: IsoDateTime;
}

/** Archivo generado de un documento electrónico (RIDE, XML). */
export interface DocumentFileDto {
  fileType: string;
  url: string;
}

/** Documento electrónico (listado y detalle). */
export interface ElectronicDocumentDto {
  id: Uuid;
  issuerId?: Uuid;
  saleId?: Uuid | null;
  documentType: string;
  sequenceNumber: number;
  accessKey?: string | null;
  status: string;
  authorizationNumber?: string | null;
  authorizedAt?: IsoDateTime | null;
  referenceDocumentId?: Uuid | null;
  lastError?: string | null;
  reason?: string | null;
  createdAt: IsoDateTime;
  /** Historial de etapas (el backend lo envía solo en el detalle; el api-client lo rellena con []). */
  events: DocumentEventDto[];
  /** Archivos generados (XML, RIDE); idem. */
  files: DocumentFileDto[];
}

/** Detalle de documento electrónico. */
export interface ElectronicDocumentDetailDto extends ElectronicDocumentDto {
  reason?: string | null;
  events: DocumentEventDto[];
  files: DocumentFileDto[];
}

/** Solicitud de emisión de documento electrónico. */
export interface IssueDocumentRequest {
  saleId: Uuid;
  documentType?: string;
}

/** Respuesta de emisión (202 Accepted). */
export interface IssueDocumentResponse {
  documentId: Uuid;
  status: string;
}

/** Solicitud de nota de crédito. */
export interface CreditNoteRequest {
  saleId: Uuid;
  reasonCode: string;
  reason: string;
}
