package ec.com.megalujos.common.print.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.com.megalujos.common.audit.AuditService;
import ec.com.megalujos.common.exception.BusinessRuleException;
import ec.com.megalujos.common.exception.ResourceNotFoundException;
import ec.com.megalujos.common.print.domain.PrintJob;
import ec.com.megalujos.common.print.domain.PrintJobRepository;
import ec.com.megalujos.common.print.dto.PrintJobRequest;
import ec.com.megalujos.common.print.dto.PrintJobResponseDto;
import ec.com.megalujos.common.web.PageResponse;
import ec.com.megalujos.pos.domain.Sale;
import ec.com.megalujos.pos.domain.SaleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PrintJobService {

    private final PrintJobRepository printJobRepository;
    private final SaleRepository saleRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final String defaultPrinter;

    public PrintJobService(PrintJobRepository printJobRepository,
                           SaleRepository saleRepository,
                           AuditService auditService,
                           ObjectMapper objectMapper,
                           @Value("${megalujos.printing.default-printer:}") String defaultPrinter) {
        this.printJobRepository = printJobRepository;
        this.saleRepository = saleRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
        this.defaultPrinter = defaultPrinter;
    }

    @Transactional
    public PrintJobResponseDto create(PrintJobRequest request, UUID actorId, String ipAddress) {
        PrintJob job = new PrintJob();
        job.setJobType(request.getJobType());
        job.setSourceType(request.getSourceType());
        job.setSourceId(request.getSourceId());
        job.setFormat(request.getFormat());
        job.setCopies(request.getCopies() > 0 ? request.getCopies() : 1);
        job.setPrinterName(request.getPrinterName() != null && !request.getPrinterName().isBlank()
                ? request.getPrinterName() : (defaultPrinter.isBlank() ? null : defaultPrinter));
        job.setRequestedBy(actorId);

        Map<String, Object> payload = request.getPayload();
        if ((payload == null || payload.isEmpty()) && "SALE_TICKET".equals(request.getJobType())) {
            payload = buildSaleTicketPayload(request.getSourceId());
        }
        try {
            job.setPayload(objectMapper.writeValueAsString(payload != null ? payload : Map.of()));
        } catch (Exception ex) {
            throw new BusinessRuleException("INVALID_PAYLOAD", "No se pudo serializar el payload: " + ex.getMessage());
        }

        PrintJob saved = printJobRepository.save(job);
        auditService.record(actorId, "PRINT_JOB", "PrintJob", saved.getId(),
                Map.of("jobType", saved.getJobType(), "sourceId", saved.getSourceId(),
                        "format", saved.getFormat(), "copies", saved.getCopies()), ipAddress);
        return PrintJobResponseDto.from(saved, objectMapper);
    }

    /** Ticket de venta: si el cliente no envió payload, se rellena con datos de la venta. */
    private Map<String, Object> buildSaleTicketPayload(UUID saleId) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Venta", saleId));
        Map<String, Object> payload = new HashMap<>();
        payload.put("saleId", sale.getId());
        payload.put("saleNumber", sale.getSaleNumber());
        payload.put("subtotal", sale.getSubtotal());
        payload.put("discountTotal", sale.getDiscountTotal());
        payload.put("taxTotal", sale.getTaxTotal());
        payload.put("grandTotal", sale.getGrandTotal());
        payload.put("status", sale.getStatus());
        payload.put("createdAt", sale.getCreatedAt().toString());
        payload.put("items", sale.getItems().stream().map(item -> {
            Map<String, Object> line = new HashMap<>();
            line.put("description", item.getDescription());
            line.put("quantity", item.getQuantity());
            line.put("unitPrice", item.getUnitPrice());
            line.put("lineTotal", item.getLineTotal());
            return line;
        }).toList());
        payload.put("payments", sale.getPayments().stream().map(payment -> {
            Map<String, Object> pay = new HashMap<>();
            pay.put("method", payment.getMethod());
            pay.put("amount", payment.getAmount());
            return pay;
        }).toList());
        return payload;
    }

    @Transactional(readOnly = true)
    public PageResponse<PrintJobResponseDto> list(String status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<PrintJob> jobs = (status == null || status.isBlank())
                ? printJobRepository.findAllByOrderByCreatedAtDesc(pageable)
                : printJobRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        return PageResponse.from(jobs, job -> PrintJobResponseDto.from(job, objectMapper));
    }

    @Transactional
    public PrintJobResponseDto markPrinted(UUID id, UUID actorId, String ipAddress) {
        PrintJob job = printJobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trabajo de impresión", id));
        if (!"PENDING".equals(job.getStatus())) {
            throw new BusinessRuleException("PRINT_JOB_NOT_PENDING",
                    "Solo se puede marcar como impreso un trabajo PENDING (estado actual: " + job.getStatus() + ")");
        }
        job.setStatus("PRINTED");
        job.setPrintedAt(OffsetDateTime.now());
        PrintJob saved = printJobRepository.save(job);
        auditService.record(actorId, "PRINT_JOB", "PrintJob", saved.getId(),
                Map.of("action", "PRINTED"), ipAddress);
        return PrintJobResponseDto.from(saved, objectMapper);
    }
}
