import { Injectable } from '@angular/core';
import {
  CreditNoteRequest,
  ElectronicDocumentDetailDto,
  ElectronicDocumentDto,
  IssuerDto,
  IssuerUpdateRequest,
  IssueDocumentRequest,
  IssueDocumentResponse,
  PageResponse,
} from '@megalujos/models';
import { Observable, map } from 'rxjs';
import { ApiBaseService, ListParams } from './api-base';

/** Parámetros de listado de documentos electrónicos. */
export interface ElectronicDocumentListParams extends ListParams {
  status?: string;
  documentType?: string;
  dateFrom?: string;
  dateTo?: string;
}

/** Regla fiscal configurada (IVA u otra). */
export interface TaxRuleDto {
  code: string;
  name: string;
  rate: string;
  validFrom?: string;
  validTo?: string;
  defaultRule?: boolean;
}

/** Configuración de facturación electrónica. */
export interface InvoicingConfigDto {
  issuer: IssuerDto | null;
  sriEnvironment?: string;
  sriReceptionUrl?: string;
  sriAuthorizationUrl?: string;
  sriEnabled?: boolean;
  taxRules?: TaxRuleDto[];
}

function withSummaryCollections(document: ElectronicDocumentDto): ElectronicDocumentDto {
  return { ...document, events: document.events ?? [], files: document.files ?? [] };
}

/**
 * Facturación electrónica: emisores, emisión, notas de crédito, reintentos y RIDE.
 * La emisión usa cabecera `Idempotency-Key` obligatoria.
 */
@Injectable({ providedIn: 'root' })
export class InvoicingService extends ApiBaseService {
  /** Configuración de facturación (emisor activo, ambiente SRI, reglas fiscales). */
  config(): Observable<InvoicingConfigDto> {
    return this.httpGet<InvoicingConfigDto>('/invoicing/config');
  }

  /** Alias de `config`. */
  getConfig(): Observable<InvoicingConfigDto> {
    return this.config();
  }

  /** Emisor tributario activo. */
  issuers(): Observable<IssuerDto> {
    return this.httpGet<IssuerDto>('/issuers');
  }

  /** Crea o actualiza el emisor (sin id). */
  updateIssuer(request: IssuerUpdateRequest): Observable<IssuerDto> {
    return this.httpPut<IssuerDto>('/issuers', request);
  }

  /** Crea o actualiza un emisor por id. */
  updateIssuerById(id: string, request: IssuerUpdateRequest): Observable<IssuerDto> {
    return this.httpPut<IssuerDto>(`/issuers/${id}`, request);
  }

  /** Listado paginado de documentos electrónicos. */
  listDocuments(params?: ElectronicDocumentListParams): Observable<PageResponse<ElectronicDocumentDto>> {
    return this.getPaged<ElectronicDocumentDto>('/electronic-documents', params).pipe(
      map((page) => ({ ...page, items: page.items.map(withSummaryCollections) })),
    );
  }

  /** Detalle de documento: eventos, mensajes, autorización y archivos. */
  getDocument(id: string): Observable<ElectronicDocumentDetailDto> {
    return this.httpGet<ElectronicDocumentDetailDto>(`/electronic-documents/${id}`);
  }

  /** Alias de `getDocument`. */
  getDocumentById(id: string): Observable<ElectronicDocumentDetailDto> {
    return this.getDocument(id);
  }

  /**
   * Solicita la emisión de un documento electrónico (202 Accepted).
   * @param idempotencyKey clave única (UUID) obligatoria en cabecera.
   */
  issue(saleId: string, documentType: string, idempotencyKey: string): Observable<IssueDocumentResponse> {
    const request: IssueDocumentRequest = { saleId, documentType };
    return this.httpPost<IssueDocumentResponse>('/electronic-documents', request, {
      headers: this.idempotencyHeaders(idempotencyKey),
    });
  }

  /** Emisión con el objeto completo. */
  issueDocument(request: IssueDocumentRequest, idempotencyKey: string): Observable<IssueDocumentResponse> {
    return this.httpPost<IssueDocumentResponse>('/electronic-documents', request, {
      headers: this.idempotencyHeaders(idempotencyKey),
    });
  }

  /** Reintenta el procesamiento de un documento con error. */
  retry(id: string): Observable<IssueDocumentResponse> {
    return this.httpPost<IssueDocumentResponse>(`/electronic-documents/${id}/retry`, {});
  }

  /** Alias de `retry`. */
  retryDocument(id: string): Observable<IssueDocumentResponse> {
    return this.retry(id);
  }

  /** Emite una nota de crédito vinculada a una venta. */
  creditNote(request: CreditNoteRequest, idempotencyKey?: string): Observable<IssueDocumentResponse> {
    return this.httpPost<IssueDocumentResponse>('/electronic-documents/credit-notes', request, {
      headers: this.idempotencyHeaders(idempotencyKey),
    });
  }

  /** Alias de `creditNote`. */
  createCreditNote(request: CreditNoteRequest, idempotencyKey: string): Observable<IssueDocumentResponse> {
    return this.creditNote(request, idempotencyKey);
  }

  /** Descarga el RIDE (PDF) como Blob. */
  ride(id: string): Observable<Blob> {
    return this.http
      .get(this.url(`/electronic-documents/${id}/ride`), { responseType: 'blob' })
      .pipe(this.handle<Blob>());
  }

  /** Alias de `ride`. */
  downloadRide(id: string): Observable<Blob> {
    return this.ride(id);
  }
}
