package ec.com.megalujos.invoicing.sri;

import ec.com.megalujos.invoicing.application.ElectronicDocumentService;
import ec.com.megalujos.invoicing.domain.*;
import ec.com.megalujos.invoicing.dto.IssueDocumentRequest;
import ec.com.megalujos.pos.domain.Sale;
import ec.com.megalujos.pos.domain.SaleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Worker outbox de facturación electrónica. Reclama eventos PENDING vencidos de
 * forma atómica (PENDING → PROCESSING con locked_until y attempts+1), genera el
 * XML, registra el evento stage=GENERATED y, solo si sri.enabled=true y el
 * certificado PKCS12 está presente, intenta firma y envío. NUNCA marca AUTHORIZED
 * sin respuesta oficial del SRI. Reintenta con backoff exponencial
 * (2^attempts minutos) y marca FAILED tras max-attempts.
 *
 * El POS inserta INVOICE_REQUESTED con aggregateId = saleId (sin comprobante
 * aún): en ese caso el worker emite el comprobante con ElectronicDocumentService
 * y completa el evento; el outbox creado por la emisión (aggregateId = documentId)
 * continúa con XML, firma y envío.
 */
@Component
public class SriOutboxWorker {

    private static final Logger log = LoggerFactory.getLogger(SriOutboxWorker.class);
    private static final long LOCK_MINUTES = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final ElectronicDocumentRepository documentRepository;
    private final ElectronicDocumentEventRepository documentEventRepository;
    private final IssuerRepository issuerRepository;
    private final SaleRepository saleRepository;
    private final CustomerSnapshotReader customerSnapshotReader;
    private final SriXmlBuilder xmlBuilder;
    private final SriClient sriClient;
    private final SriSignatureService signatureService;
    private final ElectronicDocumentService electronicDocumentService;
    private final TransactionTemplate transactionTemplate;
    private final boolean sriEnabled;
    private final String certificatePath;
    private final int maxAttempts;

    public SriOutboxWorker(OutboxEventRepository outboxEventRepository,
                           ElectronicDocumentRepository documentRepository,
                           ElectronicDocumentEventRepository documentEventRepository,
                           IssuerRepository issuerRepository,
                           SaleRepository saleRepository,
                           CustomerSnapshotReader customerSnapshotReader,
                           SriXmlBuilder xmlBuilder,
                           SriClient sriClient,
                           SriSignatureService signatureService,
                           ElectronicDocumentService electronicDocumentService,
                           PlatformTransactionManager transactionManager,
                           @Value("${megalujos.sri.enabled:false}") boolean sriEnabled,
                           @Value("${megalujos.sri.certificate-path:./certificates/signature.p12}") String certificatePath,
                           @Value("${megalujos.sri.worker.max-attempts:5}") int maxAttempts) {
        this.outboxEventRepository = outboxEventRepository;
        this.documentRepository = documentRepository;
        this.documentEventRepository = documentEventRepository;
        this.issuerRepository = issuerRepository;
        this.saleRepository = saleRepository;
        this.customerSnapshotReader = customerSnapshotReader;
        this.xmlBuilder = xmlBuilder;
        this.sriClient = sriClient;
        this.signatureService = signatureService;
        this.electronicDocumentService = electronicDocumentService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.sriEnabled = sriEnabled;
        this.certificatePath = certificatePath;
        this.maxAttempts = maxAttempts;
    }

    @Scheduled(fixedDelayString = "${megalujos.sri.worker.poll-interval-ms:10000}")
    public void poll() {
        List<OutboxEvent> due = outboxEventRepository.findDueEvents(OffsetDateTime.now());
        for (OutboxEvent event : due) {
            OffsetDateTime now = OffsetDateTime.now();
            Boolean claimed = transactionTemplate.execute(tx ->
                    outboxEventRepository.claimEvent(event.getId(),
                            now.plusMinutes(LOCK_MINUTES), now) > 0);
            if (!Boolean.TRUE.equals(claimed)) {
                continue;
            }
            try {
                processEvent(event.getId());
            } catch (Exception ex) {
                log.error("Error procesando outbox event {}: {}", event.getId(), ex.getMessage(), ex);
                handleFailure(event.getId(), "Error inesperado: " + ex.getMessage());
            }
        }
    }

    private void processEvent(UUID eventId) {
        var loaded = transactionTemplate.execute(tx -> outboxEventRepository.findById(eventId).orElse(null));
        if (loaded == null) {
            return;
        }
        UUID aggregateId = loaded.getAggregateId();

        // (0) Evento insertado por el POS al confirmar la venta: aggregateId es la
        // venta (aún no existe comprobante). Se emite aquí y se completa el evento;
        // el outbox creado por la emisión (aggregateId = documentId) continúa con
        // XML, firma y envío. Los eventos con aggregateId de comprobante no entran
        // en esta rama.
        boolean isDocument = Boolean.TRUE.equals(
                transactionTemplate.execute(tx -> documentRepository.existsById(aggregateId)));
        if (!isDocument) {
            if (!"INVOICE_REQUESTED".equals(loaded.getEventType())) {
                throw new IllegalStateException("No existe comprobante para el evento "
                        + loaded.getEventType() + " (aggregateId=" + aggregateId + ")");
            }
            emitInvoiceFromSaleEvent(loaded);
            return;
        }

        UUID documentId = aggregateId;
        var ctx = transactionTemplate.execute(tx -> loadContext(documentId));
        if (ctx == null) {
            return;
        }

        // (a) Generar XML
        String xml;
        try {
            xml = "04".equals(ctx.document.getDocumentType())
                    ? xmlBuilder.buildCreditNoteXml(ctx.document, ctx.issuer, ctx.sale, ctx.customer, ctx.sale.getItems(),
                    ctx.referenceDocument, ctx.document.getReason())
                    : xmlBuilder.buildInvoiceXml(ctx.document, ctx.issuer, ctx.sale, ctx.customer, ctx.sale.getItems());
        } catch (Exception ex) {
            handleFailure(eventId, "Error generando XML: " + ex.getMessage());
            return;
        }

        // (b) Guardar XML como evento stage=GENERATED
        transactionTemplate.execute(tx -> {
            documentEventRepository.save(new ElectronicDocumentEvent(
                    ctx.document.getId(), "GENERATED", "OK", null, xml));
            return null;
        });

        // (c) Integración deshabilitada: no se simula autorización
        if (!sriEnabled) {
            transactionTemplate.execute(tx -> {
                ElectronicDocument doc = documentRepository.findById(ctx.document.getId()).orElse(null);
                if (doc != null) {
                    doc.setStatus("GENERATED");
                    doc.setLastError("Integración SRI deshabilitada (sri.enabled=false)");
                    documentRepository.save(doc);
                }
                documentEventRepository.save(new ElectronicDocumentEvent(
                        ctx.document.getId(), "SIGNATURE", "SKIPPED", null,
                        "Integración SRI deshabilitada: firma y envío omitidos"));
                outboxEventRepository.finishEvent(eventId, "COMPLETED",
                        "Integración SRI deshabilitada (sri.enabled=false)");
                return null;
            });
            return;
        }

        // Certificado PKCS12 obligatorio para firma real: sin él el documento
        // NO se firma ni se envía, y queda GENERATED con last_error explicativo
        // (nunca se marca AUTHORIZED sin respuesta oficial).
        Path cert = Paths.get(certificatePath);
        if (!Files.exists(cert)) {
            String error = "Certificado PKCS12 no encontrado en '" + certificatePath
                    + "': configure megalujos.sri.certificate-path y password para habilitar la firma";
            transactionTemplate.execute(tx -> {
                ElectronicDocument doc = documentRepository.findById(ctx.document.getId()).orElse(null);
                if (doc != null) {
                    doc.setStatus("GENERATED");
                    doc.setLastError(error);
                    documentRepository.save(doc);
                }
                documentEventRepository.save(new ElectronicDocumentEvent(
                        ctx.document.getId(), "SIGNATURE", "SKIPPED", null, error));
                outboxEventRepository.finishEvent(eventId, "COMPLETED", error);
                return null;
            });
            return;
        }

        byte[] signedXml;
        try {
            // (d) Firma real: implementación pendiente de ficha técnica SRI (no se finge)
            signedXml = signatureService.sign(xml.getBytes(StandardCharsets.UTF_8));
            transactionTemplate.execute(tx -> {
                documentEventRepository.save(new ElectronicDocumentEvent(
                        ctx.document.getId(), "SIGNATURE", "OK", null, null));
                return null;
            });
        } catch (Exception ex) {
            handleFailure(eventId, "Firma XAdES no disponible: " + ex.getMessage());
            return;
        }

        // Envío a recepción
        SriClient.SriReceptionResponse reception;
        try {
            reception = sriClient.send(ctx.document.getAccessKey(), signedXml);
        } catch (Exception ex) {
            handleFailure(eventId, "Fallo en recepción SRI: " + ex.getMessage());
            return;
        }

        if (!reception.accepted()) {
            transactionTemplate.execute(tx -> {
                ElectronicDocument doc = documentRepository.findById(ctx.document.getId()).orElse(null);
                if (doc != null) {
                    doc.setStatus("REJECTED");
                    doc.setLastError("Recepción SRI rechazó el comprobante: " + reception.message());
                    documentRepository.save(doc);
                }
                documentEventRepository.save(new ElectronicDocumentEvent(
                        ctx.document.getId(), "RECEPTION", "REJECTED", reception.state(), reception.message()));
                outboxEventRepository.finishEvent(eventId, "COMPLETED", null);
                return null;
            });
            return;
        }

        transactionTemplate.execute(tx -> {
            ElectronicDocument doc = documentRepository.findById(ctx.document.getId()).orElse(null);
            if (doc != null) {
                doc.setStatus("SENT");
                documentRepository.save(doc);
            }
            documentEventRepository.save(new ElectronicDocumentEvent(
                    ctx.document.getId(), "RECEPTION", "RECEIVED", reception.state(), reception.message()));
            return null;
        });

        // Consulta de autorización (solo se marca AUTHORIZED con respuesta oficial)
        SriClient.SriAuthorizationResponse authorization;
        try {
            authorization = sriClient.queryAuthorization(ctx.document.getAccessKey());
        } catch (Exception ex) {
            handleFailure(eventId, "Fallo consultando autorización SRI: " + ex.getMessage());
            return;
        }

        transactionTemplate.execute(tx -> {
            ElectronicDocument doc = documentRepository.findById(ctx.document.getId()).orElse(null);
            if (authorization.authorized()) {
                if (doc != null) {
                    doc.setStatus("AUTHORIZED");
                    doc.setAuthorizationNumber(authorization.authorizationNumber());
                    doc.setAuthorizedAt(OffsetDateTime.now());
                    doc.setLastError(null);
                    documentRepository.save(doc);
                }
                documentEventRepository.save(new ElectronicDocumentEvent(
                        ctx.document.getId(), "AUTHORIZATION", "AUTHORIZED",
                        authorization.authorizationNumber(), authorization.message()));
                outboxEventRepository.finishEvent(eventId, "COMPLETED", null);
            } else {
                if (doc != null) {
                    doc.setStatus("REJECTED");
                    doc.setLastError("Comprobante no autorizado: " + authorization.message());
                    documentRepository.save(doc);
                }
                documentEventRepository.save(new ElectronicDocumentEvent(
                        ctx.document.getId(), "RECEPTION", "REJECTED", null, authorization.message()));
                outboxEventRepository.finishEvent(eventId, "COMPLETED", authorization.message());
            }
            return null;
        });
    }

    /**
     * Emite el comprobante (tipo '01') de una venta cuyo evento INVOICE_REQUESTED
     * fue insertado por el POS con aggregateId = saleId. Idempotente: si la venta
     * ya tiene comprobante solo completa el evento; si la emisión falla, la
     * excepción sube al caller y handleFailure reprograma con backoff (nunca
     * queda en bucle silencioso). El outbox creado por issueInvoice
     * (aggregateId = documentId) continúa con la generación del XML.
     */
    private void emitInvoiceFromSaleEvent(OutboxEvent event) {
        UUID saleId = event.getAggregateId();
        List<ElectronicDocument> issued = documentRepository.findBySaleIdAndDocumentType(saleId, "01");
        if (issued.isEmpty()) {
            IssueDocumentRequest request = new IssueDocumentRequest();
            request.setSaleId(saleId);
            request.setDocumentType("01");
            electronicDocumentService.issueInvoice(request, event.getId(), null, null);
        }
        transactionTemplate.execute(tx -> {
            outboxEventRepository.finishEvent(event.getId(), "COMPLETED",
                    "Comprobante emitido para la venta; outbox de facturación encolado");
            return null;
        });
    }

    private void handleFailure(UUID eventId, String errorMessage) {
        transactionTemplate.execute(tx -> {
            OutboxEvent event = outboxEventRepository.findById(eventId).orElse(null);
            if (event == null) {
                return null;
            }
            ElectronicDocument doc = documentRepository.findById(event.getAggregateId()).orElse(null);
            if (doc != null && !"AUTHORIZED".equals(doc.getStatus())) {
                doc.setLastError(errorMessage);
                documentRepository.save(doc);
            }
            int attempts = event.getAttempts();
            if (attempts >= maxAttempts) {
                if (doc != null && !"AUTHORIZED".equals(doc.getStatus())) {
                    doc.setStatus("FAILED");
                    documentRepository.save(doc);
                }
                outboxEventRepository.finishEvent(eventId, "FAILED", errorMessage);
                log.error("Outbox event {} FAILED tras {} intentos: {}", eventId, attempts, errorMessage);
            } else {
                long backoffMinutes = 1L << Math.min(attempts, 20); // 2^attempts minutos
                OffsetDateTime nextAttempt = OffsetDateTime.now().plusMinutes(backoffMinutes);
                outboxEventRepository.rescheduleEvent(eventId, nextAttempt, errorMessage);
                log.warn("Outbox event {} reprogramado (intento {}): {}", eventId, attempts, errorMessage);
            }
            return null;
        });
    }

    private record Context(ElectronicDocument document, Issuer issuer, Sale sale,
                           CustomerSnapshotReader.CustomerSnapshot customer,
                           ElectronicDocument referenceDocument) {
    }

    private Context loadContext(UUID documentId) {
        ElectronicDocument document = documentRepository.findById(documentId).orElse(null);
        if (document == null) {
            return null;
        }
        Issuer issuer = issuerRepository.findById(document.getIssuerId()).orElse(null);
        Sale sale = document.getSaleId() != null
                ? saleRepository.findById(document.getSaleId()).orElse(null) : null;
        if (issuer == null || sale == null) {
            throw new IllegalStateException("Emisor o venta no encontrados para el documento " + documentId);
        }
        CustomerSnapshotReader.CustomerSnapshot customer =
                customerSnapshotReader.findByCustomerId(sale.getCustomerId());
        ElectronicDocument reference = document.getReferenceDocumentId() != null
                ? documentRepository.findById(document.getReferenceDocumentId()).orElse(null) : null;
        return new Context(document, issuer, sale, customer, reference);
    }
}
