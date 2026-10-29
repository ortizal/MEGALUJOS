package ec.com.megalujos.invoicing.sri;

import ec.com.megalujos.invoicing.application.ElectronicDocumentService;
import ec.com.megalujos.invoicing.domain.CustomerSnapshotReader;
import ec.com.megalujos.invoicing.domain.ElectronicDocument;
import ec.com.megalujos.invoicing.domain.ElectronicDocumentEvent;
import ec.com.megalujos.invoicing.domain.ElectronicDocumentEventRepository;
import ec.com.megalujos.invoicing.domain.ElectronicDocumentRepository;
import ec.com.megalujos.invoicing.domain.Issuer;
import ec.com.megalujos.invoicing.domain.IssuerRepository;
import ec.com.megalujos.invoicing.domain.OutboxEvent;
import ec.com.megalujos.invoicing.domain.OutboxEventRepository;
import ec.com.megalujos.invoicing.dto.IssueDocumentRequest;
import ec.com.megalujos.pos.domain.Sale;
import ec.com.megalujos.pos.domain.SaleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios del SriOutboxWorker: eventos INVOICE_REQUESTED insertados por
 * el POS (aggregateId = saleId sin comprobante) deben emitir el comprobante y
 * completarse; eventos no resolubles deben reprogramarse (nunca bucle silencioso);
 * con sri.enabled=false el documento queda GENERATED sin firma ni envío.
 */
@ExtendWith(MockitoExtension.class)
class SriOutboxWorkerTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Mock
    private ElectronicDocumentRepository documentRepository;
    @Mock
    private ElectronicDocumentEventRepository documentEventRepository;
    @Mock
    private IssuerRepository issuerRepository;
    @Mock
    private SaleRepository saleRepository;
    @Mock
    private CustomerSnapshotReader customerSnapshotReader;
    @Mock
    private SriXmlBuilder xmlBuilder;
    @Mock
    private SriClient sriClient;
    @Mock
    private SriSignatureService signatureService;
    @Mock
    private ElectronicDocumentService electronicDocumentService;
    @Mock
    private PlatformTransactionManager transactionManager;

    /** Worker con sri.enabled=false (comportamiento por defecto del proyecto). */
    private SriOutboxWorker worker;

    private final UUID eventId = UUID.randomUUID();
    private final UUID saleId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        worker = new SriOutboxWorker(outboxEventRepository, documentRepository, documentEventRepository,
                issuerRepository, saleRepository, customerSnapshotReader, xmlBuilder, sriClient,
                signatureService, electronicDocumentService, transactionManager,
                false, "./certificates/signature.p12", 5);
    }

    private OutboxEvent evento(String tipo, UUID aggregateId) {
        OutboxEvent event = new OutboxEvent(tipo, aggregateId, "{}");
        event.setId(eventId);
        return event;
    }

    private void prepararCiclo(OutboxEvent event) {
        when(outboxEventRepository.findDueEvents(any(OffsetDateTime.class))).thenReturn(List.of(event));
        when(outboxEventRepository.claimEvent(eq(eventId), any(OffsetDateTime.class),
                any(OffsetDateTime.class))).thenReturn(1);
        when(outboxEventRepository.findById(eventId)).thenReturn(Optional.of(event));
    }

    /** Evento del POS (aggregateId = saleId, sin comprobante): debe emitirlo y completar el evento. */
    @Test
    void eventoInvoiceRequestedDesdePos_emiteComprobante_yCompletaElEvento() {
        OutboxEvent event = evento("INVOICE_REQUESTED", saleId);
        prepararCiclo(event);
        // existsById(saleId) → false por defecto (la venta aún no tiene comprobante)
        when(documentRepository.findBySaleIdAndDocumentType(saleId, "01")).thenReturn(List.of());

        worker.poll();

        ArgumentCaptor<IssueDocumentRequest> captor = ArgumentCaptor.forClass(IssueDocumentRequest.class);
        verify(electronicDocumentService).issueInvoice(captor.capture(), eq(eventId), isNull(), isNull());
        assertEquals(saleId, captor.getValue().getSaleId());
        assertEquals("01", captor.getValue().getDocumentType());
        verify(outboxEventRepository).finishEvent(eq(eventId), eq("COMPLETED"), contains("Comprobante emitido"));
        verify(electronicDocumentService, times(1)).issueInvoice(any(), any(), any(), any());
    }

    /** La venta ya tiene comprobante: no duplica la emisión y aún así completa el evento. */
    @Test
    void eventoInvoiceRequested_conComprobanteExistente_noDuplica_yCompleta() {
        OutboxEvent event = evento("INVOICE_REQUESTED", saleId);
        prepararCiclo(event);
        ElectronicDocument emitido = new ElectronicDocument();
        emitido.setId(UUID.randomUUID());
        when(documentRepository.findBySaleIdAndDocumentType(saleId, "01")).thenReturn(List.of(emitido));

        worker.poll();

        verify(electronicDocumentService, never()).issueInvoice(any(), any(), any(), any());
        verify(outboxEventRepository).finishEvent(eq(eventId), eq("COMPLETED"), anyString());
    }

    /** AggregateId no resoluble y tipo distinto de INVOICE_REQUESTED: se reprograma con backoff (sin bucle silencioso). */
    @Test
    void eventoSinDocumento_yTipoNoEmisible_seReprograma() {
        OutboxEvent event = evento("CREDIT_NOTE_REQUESTED", saleId);
        event.setAttempts(1);
        prepararCiclo(event);
        // existsById(aggregateId) → false por defecto

        worker.poll();

        verify(electronicDocumentService, never()).issueInvoice(any(), any(), any(), any());
        verify(outboxEventRepository).rescheduleEvent(eq(eventId), any(OffsetDateTime.class),
                contains("No existe comprobante"));
        verify(outboxEventRepository, never()).finishEvent(any(), any(), any());
    }

    /** Con sri.enabled=false: XML generado, documento GENERATED y evento COMPLETED; nunca firma ni envía. */
    @Test
    void eventoDeDocumento_sriDeshabilitado_generaXml_sinFirmaNiEnvio() {
        UUID docId = UUID.randomUUID();
        UUID issuerId = UUID.randomUUID();
        OutboxEvent event = evento("INVOICE_REQUESTED", docId);
        prepararCiclo(event);

        ElectronicDocument doc = new ElectronicDocument();
        doc.setId(docId);
        doc.setIssuerId(issuerId);
        doc.setSaleId(saleId);
        doc.setDocumentType("01");
        doc.setStatus("GENERATED");

        Issuer issuer = new Issuer();
        issuer.setId(issuerId);
        Sale sale = new Sale();
        sale.setId(saleId);

        when(documentRepository.existsById(docId)).thenReturn(true);
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(issuerRepository.findById(issuerId)).thenReturn(Optional.of(issuer));
        when(saleRepository.findById(saleId)).thenReturn(Optional.of(sale));
        when(xmlBuilder.buildInvoiceXml(any(), any(), any(), any(), any())).thenReturn("<FacturaXml/>");

        worker.poll();

        verify(documentEventRepository, times(2)).save(any(ElectronicDocumentEvent.class));
        verify(outboxEventRepository).finishEvent(eq(eventId), eq("COMPLETED"), contains("sri.enabled=false"));
        assertEquals("GENERATED", doc.getStatus(), "El documento debe quedar GENERATED");
        verify(signatureService, never()).sign(any());
        verify(sriClient, never()).send(any(), any());
        verify(sriClient, never()).queryAuthorization(any());
    }
}
