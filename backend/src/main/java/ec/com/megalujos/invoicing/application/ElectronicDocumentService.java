package ec.com.megalujos.invoicing.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.com.megalujos.catalog.domain.MediaFileRepository;
import ec.com.megalujos.common.audit.AuditService;
import ec.com.megalujos.common.exception.BusinessRuleException;
import ec.com.megalujos.common.exception.ResourceNotFoundException;
import ec.com.megalujos.invoicing.domain.*;
import ec.com.megalujos.invoicing.dto.*;
import ec.com.megalujos.invoicing.sri.AccessKeyGenerator;
import ec.com.megalujos.pos.domain.Sale;
import ec.com.megalujos.pos.domain.SaleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ElectronicDocumentService {

    private final IssuerRepository issuerRepository;
    private final DocumentSequenceRepository documentSequenceRepository;
    private final ElectronicDocumentRepository electronicDocumentRepository;
    private final ElectronicDocumentEventRepository eventRepository;
    private final ElectronicDocumentFileRepository fileRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final SaleRepository saleRepository;
    private final MediaFileRepository mediaFileRepository;
    private final AuditService auditService;
    private final AccessKeyGenerator accessKeyGenerator;
    private final ObjectMapper objectMapper;

    public ElectronicDocumentService(IssuerRepository issuerRepository,
                                     DocumentSequenceRepository documentSequenceRepository,
                                     ElectronicDocumentRepository electronicDocumentRepository,
                                     ElectronicDocumentEventRepository eventRepository,
                                     ElectronicDocumentFileRepository fileRepository,
                                     OutboxEventRepository outboxEventRepository,
                                     SaleRepository saleRepository,
                                     MediaFileRepository mediaFileRepository,
                                     AuditService auditService,
                                     AccessKeyGenerator accessKeyGenerator,
                                     ObjectMapper objectMapper) {
        this.issuerRepository = issuerRepository;
        this.documentSequenceRepository = documentSequenceRepository;
        this.electronicDocumentRepository = electronicDocumentRepository;
        this.eventRepository = eventRepository;
        this.fileRepository = fileRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.saleRepository = saleRepository;
        this.mediaFileRepository = mediaFileRepository;
        this.auditService = auditService;
        this.accessKeyGenerator = accessKeyGenerator;
        this.objectMapper = objectMapper;
    }

    /**
     * Emite un comprobante electrónico para una venta: toma el secuencial de forma
     * atómica (SELECT FOR UPDATE sobre document_sequences), construye la clave de
     * acceso, persiste el documento en GENERATED y encola INVOICE_REQUESTED en outbox.
     */
    @Transactional
    public ElectronicDocumentSummaryDto issueInvoice(IssueDocumentRequest request, UUID idempotencyKey,
                                                     UUID actorId, String ipAddress) {
        if (request.getDocumentType() == null || !request.getDocumentType().equals("01")) {
            throw new BusinessRuleException("INVALID_DOCUMENT_TYPE",
                    "El tipo de comprobante para facturas debe ser '01'");
        }
        if (idempotencyKey != null) {
            var existing = electronicDocumentRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return ElectronicDocumentSummaryDto.from(existing.get());
            }
        }

        Sale sale = saleRepository.findById(request.getSaleId())
                .orElseThrow(() -> new ResourceNotFoundException("Venta", request.getSaleId()));
        if (!"CONFIRMED".equals(sale.getStatus()) && !"PARTIALLY_RETURNED".equals(sale.getStatus())) {
            throw new BusinessRuleException("SALE_NOT_ISSUABLE",
                    "Solo se pueden emitir comprobantes de ventas confirmadas (estado actual: " + sale.getStatus() + ")");
        }

        List<ElectronicDocument> alreadyIssued =
                electronicDocumentRepository.findBySaleIdAndDocumentType(sale.getId(), "01");
        if (!alreadyIssued.isEmpty()) {
            throw new BusinessRuleException("DOCUMENT_ALREADY_ISSUED",
                    "La venta ya tiene un comprobante emitido (documento " + alreadyIssued.get(0).getId() + ")");
        }

        ElectronicDocument document = createDocument(sale, "01", null, null, idempotencyKey);
        enqueueOutbox(document, "INVOICE_REQUESTED");
        auditService.record(actorId, "INVOICE_REQUESTED", "ElectronicDocument", document.getId(),
                Map.of("saleId", sale.getId(), "accessKey", document.getAccessKey()), ipAddress);
        return ElectronicDocumentSummaryDto.from(document);
    }

    /**
     * Nota de crédito (tipo '04'): secuencia propia, referencia a la factura
     * electrónica original de la misma venta. Solo cola (outbox CREDIT_NOTE_REQUESTED).
     */
    @Transactional
    public ElectronicDocumentSummaryDto issueCreditNote(CreditNoteRequest request, UUID idempotencyKey,
                                                        UUID actorId, String ipAddress) {
        if (idempotencyKey != null) {
            var existing = electronicDocumentRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return ElectronicDocumentSummaryDto.from(existing.get());
            }
        }

        Sale sale = saleRepository.findById(request.getSaleId())
                .orElseThrow(() -> new ResourceNotFoundException("Venta", request.getSaleId()));

        ElectronicDocument original = electronicDocumentRepository
                .findOriginalInvoices(sale.getId(), List.of("AUTHORIZED", "GENERATED")).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("ORIGINAL_INVOICE_NOT_FOUND",
                        "La venta no tiene factura electrónica (AUTHORIZED o GENERATED) que referenciar"));

        ElectronicDocument document = createDocument(sale, "04", original.getId(),
                "[" + request.getReasonCode() + "] " + request.getReason(), idempotencyKey);
        enqueueOutbox(document, "CREDIT_NOTE_REQUESTED");
        auditService.record(actorId, "CREDIT_NOTE_REQUESTED", "ElectronicDocument", document.getId(),
                Map.of("saleId", sale.getId(), "referenceDocumentId", original.getId(),
                        "reasonCode", request.getReasonCode()), ipAddress);
        return ElectronicDocumentSummaryDto.from(document);
    }

    private ElectronicDocument createDocument(Sale sale, String documentType, UUID referenceDocumentId,
                                              String reason, UUID idempotencyKey) {
        Issuer issuer = issuerRepository.findFirstByActiveTrue()
                .orElseThrow(() -> new BusinessRuleException("NO_ACTIVE_ISSUER",
                        "No existe un emisor activo configurado para facturación electrónica"));

        long sequenceNumber = nextSequence(issuer, documentType);
        String accessKey = accessKeyGenerator.build(
                issuer.getTaxId(), documentType, issuer.getEstablishmentCode(),
                issuer.getEmissionPointCode(), sequenceNumber, issuer.getEnvironment(),
                sale.getCreatedAt().toLocalDate());

        ElectronicDocument document = new ElectronicDocument();
        document.setIssuerId(issuer.getId());
        document.setSaleId(sale.getId());
        document.setDocumentType(documentType);
        document.setSequenceNumber(sequenceNumber);
        document.setAccessKey(accessKey);
        document.setStatus("GENERATED");
        document.setReferenceDocumentId(referenceDocumentId);
        document.setReason(reason);
        document.setIdempotencyKey(idempotencyKey);
        return electronicDocumentRepository.save(document);
    }

    /**
     * Incremento atómico del secuencial bajo bloqueo pesimista. Si la fila no
     * existe se crea con next_number=2 y se devuelve el 1; la restricción
     * UNIQUE(issuer_id, document_type) protege contra carreras de inserción.
     */
    private long nextSequence(Issuer issuer, String documentType) {
        var locked = documentSequenceRepository.findByIssuerAndTypeForUpdate(issuer.getId(), documentType);
        if (locked.isEmpty()) {
            DocumentSequence created = new DocumentSequence(issuer.getId(), documentType, 2);
            documentSequenceRepository.save(created);
            return 1;
        }
        DocumentSequence sequence = locked.get();
        long current = sequence.getNextNumber();
        sequence.setNextNumber(current + 1);
        documentSequenceRepository.save(sequence);
        return current;
    }

    private void enqueueOutbox(ElectronicDocument document, String eventType) {
        try {
            Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("documentId", document.getId());
            payload.put("documentType", document.getDocumentType());
            if (document.getSaleId() != null) {
                payload.put("saleId", document.getSaleId());
            }
            String json = objectMapper.writeValueAsString(payload);
            outboxEventRepository.save(new OutboxEvent(eventType, document.getId(), json));
        } catch (Exception ex) {
            throw new BusinessRuleException("OUTBOX_ERROR", "No se pudo encolar el evento: " + ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<ElectronicDocumentSummaryDto> list(String status, String documentType,
                                                   OffsetDateTime dateFrom, OffsetDateTime dateTo,
                                                   int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return electronicDocumentRepository.search(status, documentType, dateFrom, dateTo, pageable)
                .map(ElectronicDocumentSummaryDto::from);
    }

    @Transactional(readOnly = true)
    public ElectronicDocumentDetailDto getDetail(UUID id) {
        ElectronicDocument document = electronicDocumentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comprobante electrónico", id));

        ElectronicDocumentDetailDto dto = new ElectronicDocumentDetailDto();
        ElectronicDocumentSummaryDto summary = ElectronicDocumentSummaryDto.from(document);
        dto.setId(summary.getId());
        dto.setIssuerId(summary.getIssuerId());
        dto.setSaleId(summary.getSaleId());
        dto.setDocumentType(summary.getDocumentType());
        dto.setSequenceNumber(summary.getSequenceNumber());
        dto.setAccessKey(summary.getAccessKey());
        dto.setStatus(summary.getStatus());
        dto.setAuthorizationNumber(summary.getAuthorizationNumber());
        dto.setAuthorizedAt(summary.getAuthorizedAt());
        dto.setReferenceDocumentId(summary.getReferenceDocumentId());
        dto.setLastError(summary.getLastError());
        dto.setCreatedAt(summary.getCreatedAt());
        dto.setReason(document.getReason());

        for (ElectronicDocumentEvent event : eventRepository.findByDocumentIdOrderByOccurredAtAsc(id)) {
            DocumentEventDto eventDto = new DocumentEventDto();
            eventDto.setId(event.getId());
            eventDto.setStage(event.getStage());
            eventDto.setStatus(event.getStatus());
            eventDto.setResponseCode(event.getResponseCode());
            eventDto.setOccurredAt(event.getOccurredAt());
            dto.getEvents().add(eventDto);
        }

        for (String fileType : List.of("XML_AUTHORIZED", "RIDE_PDF")) {
            fileRepository.findByDocumentIdAndFileType(id, fileType).ifPresent(file ->
                    mediaFileRepository.findById(file.getMediaId()).ifPresent(media ->
                            dto.getFiles().add(new DocumentFileDto(fileType, "/uploads/" + media.getStorageKey()))));
        }
        return dto;
    }

    /**
     * Reintento de un comprobante rechazado o fallido: conserva accessKey y
     * secuencia; vuelve a GENERATED y genera un nuevo evento outbox.
     */
    @Transactional
    public ElectronicDocumentSummaryDto retry(UUID id, UUID actorId, String ipAddress) {
        ElectronicDocument document = electronicDocumentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comprobante electrónico", id));

        if (!"REJECTED".equals(document.getStatus()) && !"FAILED".equals(document.getStatus())) {
            throw new BusinessRuleException("RETRY_NOT_ALLOWED",
                    "Solo se puede reintentar comprobantes en estado REJECTED o FAILED (estado actual: "
                            + document.getStatus() + ")");
        }

        document.setStatus("GENERATED");
        document.setLastError(null);
        electronicDocumentRepository.save(document);

        String eventType = "04".equals(document.getDocumentType()) ? "CREDIT_NOTE_REQUESTED" : "INVOICE_REQUESTED";
        enqueueOutbox(document, eventType);
        auditService.record(actorId, "ELECTRONIC_DOCUMENT_RETRIED", "ElectronicDocument", document.getId(),
                Map.of("status", "GENERATED", "eventType", eventType), ipAddress);
        return ElectronicDocumentSummaryDto.from(document);
    }

    @Transactional(readOnly = true)
    public ElectronicDocument getDocumentEntity(UUID id) {
        return electronicDocumentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comprobante electrónico", id));
    }

    @Transactional
    public void attachFile(UUID documentId, String fileType, UUID mediaId) {
        fileRepository.save(new ElectronicDocumentFile(documentId, fileType, mediaId));
    }
}
