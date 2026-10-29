package ec.com.megalujos.invoicing.api;

import ec.com.megalujos.common.security.UserPrincipal;
import ec.com.megalujos.common.web.PageResponse;
import ec.com.megalujos.invoicing.application.ElectronicDocumentService;
import ec.com.megalujos.invoicing.dto.CreditNoteRequest;
import ec.com.megalujos.invoicing.dto.ElectronicDocumentDetailDto;
import ec.com.megalujos.invoicing.dto.ElectronicDocumentSummaryDto;
import ec.com.megalujos.invoicing.dto.IssueDocumentRequest;
import ec.com.megalujos.invoicing.dto.IssueDocumentResponse;
import ec.com.megalujos.invoicing.pdf.RidePdfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/electronic-documents")
@Tag(name = "Comprobantes Electrónicos", description = "Emisión, consulta y reintento de facturas y notas de crédito electrónicas SRI")
public class ElectronicDocumentController {

    private final ElectronicDocumentService electronicDocumentService;
    private final RidePdfService ridePdfService;

    public ElectronicDocumentController(ElectronicDocumentService electronicDocumentService,
                                        RidePdfService ridePdfService) {
        this.electronicDocumentService = electronicDocumentService;
        this.ridePdfService = ridePdfService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('invoicing:issue') or hasRole('ADMIN')")
    @Operation(summary = "Emitir comprobante electrónico", description = "Genera una factura electrónica (tipo 01) para una venta con secuencial atómico y cola outbox; requiere cabecera Idempotency-Key")
    public ResponseEntity<IssueDocumentResponse> issue(
            @Parameter(description = "Clave de idempotencia (UUID) para evitar duplicados", required = true)
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody IssueDocumentRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {
        UUID actorId = principal != null ? principal.getId() : null;
        ElectronicDocumentSummaryDto result =
                electronicDocumentService.issueInvoice(request, idempotencyKey, actorId, httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(IssueDocumentResponse.from(result));
    }

    @PostMapping("/credit-notes")
    @PreAuthorize("hasAuthority('invoicing:issue') or hasRole('ADMIN')")
    @Operation(summary = "Emitir nota de crédito electrónica", description = "Genera una nota de crédito (tipo 04) que referencia la factura electrónica original de la venta; solo cola outbox; requiere Idempotency-Key")
    public ResponseEntity<IssueDocumentResponse> creditNote(
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody CreditNoteRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {
        UUID actorId = principal != null ? principal.getId() : null;
        ElectronicDocumentSummaryDto result =
                electronicDocumentService.issueCreditNote(request, idempotencyKey, actorId, httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(IssueDocumentResponse.from(result));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('invoicing:read') or hasRole('ADMIN')")
    @Operation(summary = "Listar comprobantes electrónicos", description = "Paginado con filtros por status, tipo de comprobante y rango de fechas de creación")
    public ResponseEntity<PageResponse<ElectronicDocumentSummaryDto>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String documentType,
            @RequestParam(required = false) OffsetDateTime dateFrom,
            @RequestParam(required = false) OffsetDateTime dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.from(
                electronicDocumentService.list(status, documentType, dateFrom, dateTo, page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('invoicing:read') or hasRole('ADMIN')")
    @Operation(summary = "Detalle de comprobante electrónico", description = "Incluye eventos del ciclo de vida y URLs de archivos (XML_AUTHORIZED, RIDE_PDF) cuando existen")
    public ResponseEntity<ElectronicDocumentDetailDto> getDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(electronicDocumentService.getDetail(id));
    }

    @PostMapping("/{id}/retry")
    @PreAuthorize("hasAuthority('invoicing:issue') or hasRole('ADMIN')")
    @Operation(summary = "Reintentar comprobante", description = "Solo para estado REJECTED o FAILED: conserva clave de acceso y secuencia, vuelve a GENERATED y genera nuevo evento outbox")
    public ResponseEntity<IssueDocumentResponse> retry(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {
        UUID actorId = principal != null ? principal.getId() : null;
        ElectronicDocumentSummaryDto result = electronicDocumentService.retry(id, actorId, httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(IssueDocumentResponse.from(result));
    }

    @GetMapping("/{id}/ride")
    @PreAuthorize("hasAuthority('invoicing:read') or hasRole('ADMIN')")
    @Operation(summary = "Descargar RIDE en PDF", description = "Representación impresa del comprobante; se genera y almacena la primera vez si el documento está AUTHORIZED")
    public ResponseEntity<byte[]> downloadRide(@PathVariable UUID id) {
        UUID mediaId = ridePdfService.getOrGenerateRide(id);
        byte[] pdf = ridePdfService.loadPdfBytes(mediaId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"RIDE-" + id + ".pdf\"")
                .body(pdf);
    }
}
