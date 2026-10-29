package ec.com.megalujos.services.application;

import ec.com.megalujos.catalog.domain.MediaFile;
import ec.com.megalujos.catalog.domain.MediaFileRepository;
import ec.com.megalujos.cash.domain.CashMovement;
import ec.com.megalujos.cash.domain.CashMovementRepository;
import ec.com.megalujos.cash.domain.CashSession;
import ec.com.megalujos.cash.domain.CashSessionRepository;
import ec.com.megalujos.common.audit.AuditService;
import ec.com.megalujos.common.exception.BusinessRuleException;
import ec.com.megalujos.common.exception.ResourceNotFoundException;
import ec.com.megalujos.common.tax.TaxRule;
import ec.com.megalujos.common.tax.TaxService;
import ec.com.megalujos.common.web.PageResponse;
import ec.com.megalujos.inventory.application.StockService;
import ec.com.megalujos.pos.application.SaleChargeService;
import ec.com.megalujos.pos.dto.SaleResponseDto;
import ec.com.megalujos.pos.dto.ServiceChargeResultDto;
import ec.com.megalujos.pos.dto.ServiceOrderChargeCommand;
import ec.com.megalujos.services.domain.*;
import ec.com.megalujos.services.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Órdenes de servicio: cotización, estados, materiales, anticipos y cobro.
 * Los materiales solo descuentan stock al consumirse (nunca en la cotización).
 */
@Service
public class ServiceOrderService {

    private static final ZoneId ECUADOR = ZoneId.of("America/Guayaquil");
    private static final List<String> SERVICE_TYPES = List.of("STICKER", "INSTALLATION", "OTHER");
    private static final List<String> ADVANCE_PAYMENT_METHODS = List.of("CASH", "TRANSFER", "CARD", "OTHER");

    private final ServiceOrderRepository orderRepository;
    private final ServiceMaterialRepository materialRepository;
    private final ServiceAttachmentRepository attachmentRepository;
    private final ServiceAdvanceRepository advanceRepository;
    private final MediaFileRepository mediaFileRepository;
    private final CashSessionRepository cashSessionRepository;
    private final CashMovementRepository cashMovementRepository;
    private final StockService stockService;
    private final TaxService taxService;
    private final SaleChargeService saleChargeService;
    private final AuditService auditService;

    public ServiceOrderService(ServiceOrderRepository orderRepository,
                               ServiceMaterialRepository materialRepository,
                               ServiceAttachmentRepository attachmentRepository,
                               ServiceAdvanceRepository advanceRepository,
                               MediaFileRepository mediaFileRepository,
                               CashSessionRepository cashSessionRepository,
                               CashMovementRepository cashMovementRepository,
                               StockService stockService,
                               TaxService taxService,
                               SaleChargeService saleChargeService,
                               AuditService auditService) {
        this.orderRepository = orderRepository;
        this.materialRepository = materialRepository;
        this.attachmentRepository = attachmentRepository;
        this.advanceRepository = advanceRepository;
        this.mediaFileRepository = mediaFileRepository;
        this.cashSessionRepository = cashSessionRepository;
        this.cashMovementRepository = cashMovementRepository;
        this.stockService = stockService;
        this.taxService = taxService;
        this.saleChargeService = saleChargeService;
        this.auditService = auditService;
    }

    @Transactional
    public ServiceOrderDto create(CreateServiceOrderRequest request, UUID actorId, String ipAddress) {
        String serviceType = request.getServiceType().trim().toUpperCase();
        if (!SERVICE_TYPES.contains(serviceType)) {
            throw new BusinessRuleException("INVALID_SERVICE_TYPE", "Tipo de servicio no válido: " + request.getServiceType());
        }

        ServiceOrder order = new ServiceOrder();
        String datePrefix = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        order.setOrderNumber("OTS-" + datePrefix + "-" + randomSuffix);
        order.setCustomerId(request.getCustomerId());
        order.setVehicleId(request.getVehicleId());
        order.setServiceType(serviceType);
        order.setDescription(request.getDescription());
        order.setDimensions(request.getDimensions());
        order.setScheduledAt(request.getScheduledAt());
        order.setLaborAmount(request.getLaborAmount() != null ? request.getLaborAmount() : BigDecimal.ZERO);
        order.setAssignedUserId(request.getAssignedUserId());
        order.setStatus(request.isConfirmed() ? "CONFIRMED" : "QUOTED");
        order.setCreatedAt(OffsetDateTime.now());
        order.setUpdatedAt(OffsetDateTime.now());

        if (request.getItems() != null) {
            for (CreateServiceOrderRequest.ItemRequest itemReq : request.getItems()) {
                BigDecimal lineTotal = itemReq.getQuantity().multiply(itemReq.getUnitPrice())
                        .setScale(2, RoundingMode.HALF_UP);

                ServiceOrderItem item = new ServiceOrderItem();
                item.setServiceOrder(order);
                item.setDescription(itemReq.getDescription());
                item.setQuantity(itemReq.getQuantity());
                item.setUnitPrice(itemReq.getUnitPrice());
                item.setLineTotal(lineTotal);
                order.getItems().add(item);
            }
        }

        if (request.getMaterials() != null) {
            for (CreateServiceOrderRequest.MaterialRequest materialReq : request.getMaterials()) {
                ServiceMaterial material = new ServiceMaterial();
                material.setServiceOrder(order);
                material.setVariantId(materialReq.getVariantId());
                material.setPlannedQuantity(materialReq.getPlannedQuantity() != null
                        ? materialReq.getPlannedQuantity() : BigDecimal.ZERO);
                material.setConsumedQuantity(BigDecimal.ZERO);
                order.getMaterials().add(material);
            }
        }

        recalculateEstimatedTotal(order);

        ServiceOrder saved = orderRepository.save(order);

        ServiceStatusHistory history = new ServiceStatusHistory();
        history.setServiceOrder(saved);
        history.setOldStatus(null);
        history.setNewStatus(saved.getStatus());
        history.setActorId(actorId);
        history.setNotes(saved.getStatus().equals("CONFIRMED") ? "Orden confirmada al crearse" : "Cotización creada");
        saved.getStatusHistory().add(history);

        if (request.getAttachmentIds() != null && !request.getAttachmentIds().isEmpty()) {
            List<MediaFile> medias = mediaFileRepository.findAllById(request.getAttachmentIds());
            if (medias.size() != new HashSet<>(request.getAttachmentIds()).size()) {
                throw new ResourceNotFoundException("Archivo", request.getAttachmentIds());
            }
            for (MediaFile media : medias) {
                ServiceAttachment attachment = new ServiceAttachment();
                attachment.setServiceOrderId(saved.getId());
                attachment.setMediaId(media.getId());
                attachment.setAttachmentType("DESIGN");
                attachmentRepository.save(attachment);
            }
        }

        orderRepository.save(saved);

        auditService.record(actorId, "SERVICE_ORDER_CREATED", "SERVICE_ORDER", saved.getId(), Map.of(
                "orderNumber", saved.getOrderNumber(),
                "customerId", saved.getCustomerId(),
                "estimatedTotal", saved.getEstimatedTotal(),
                "status", saved.getStatus()
        ), ipAddress);

        return toDetail(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<ServiceOrderSummaryDto> list(String status, String type, String q, UUID assignedUserId,
                                                     Pageable pageable) {
        Pageable sorted = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<ServiceOrder> page = orderRepository.search(
                normalize(status), normalize(type), assignedUserId, normalize(q), sorted);
        return PageResponse.from(page, this::toSummary);
    }

    @Transactional(readOnly = true)
    public ServiceOrderDto getDetail(UUID id) {
        return toDetail(findOrder(id));
    }

    @Transactional
    public ServiceOrderDto update(UUID id, UpdateServiceOrderRequest request, UUID actorId, String ipAddress) {
        ServiceOrder order = findOrder(id);
        if (!isEditableStatus(order.getStatus())) {
            throw new BusinessRuleException("ORDER_NOT_EDITABLE",
                    "Solo se puede editar órdenes QUOTED, CONFIRMED o IN_PROGRESS. Estado actual: " + order.getStatus());
        }

        String serviceType = request.getServiceType().trim().toUpperCase();
        if (!SERVICE_TYPES.contains(serviceType)) {
            throw new BusinessRuleException("INVALID_SERVICE_TYPE", "Tipo de servicio no válido: " + request.getServiceType());
        }

        if (request.getCustomerId() != null) {
            order.setCustomerId(request.getCustomerId());
        }
        order.setVehicleId(request.getVehicleId());
        order.setServiceType(serviceType);
        order.setDescription(request.getDescription());
        order.setDimensions(request.getDimensions());
        order.setScheduledAt(request.getScheduledAt());
        order.setLaborAmount(request.getLaborAmount() != null ? request.getLaborAmount() : BigDecimal.ZERO);
        order.setAssignedUserId(request.getAssignedUserId());

        recalculateEstimatedTotal(order);
        order.setUpdatedAt(OffsetDateTime.now());
        ServiceOrder saved = orderRepository.save(order);

        auditService.record(actorId, "SERVICE_ORDER_UPDATED", "SERVICE_ORDER", saved.getId(), Map.of(
                "orderNumber", saved.getOrderNumber(),
                "estimatedTotal", saved.getEstimatedTotal()
        ), ipAddress);

        return toDetail(saved);
    }

    @Transactional
    public ServiceOrderDto changeStatus(UUID id, ChangeStatusRequest request, UUID actorId, String ipAddress) {
        ServiceOrder order = findOrder(id);
        String current = order.getStatus();
        String target = request.getStatus() != null ? request.getStatus().trim().toUpperCase() : "";

        if (!allowedTransitions().getOrDefault(current, List.of()).contains(target)) {
            throw new BusinessRuleException("INVALID_STATUS_TRANSITION",
                    "No se puede cambiar de " + current + " a " + target);
        }

        order.setStatus(target);
        order.setUpdatedAt(OffsetDateTime.now());
        if ("DELIVERED".equals(target)) {
            order.setCompletedAt(OffsetDateTime.now());
        }

        ServiceStatusHistory history = new ServiceStatusHistory();
        history.setServiceOrder(order);
        history.setOldStatus(current);
        history.setNewStatus(target);
        history.setActorId(actorId);
        history.setNotes(request.getNotes());
        order.getStatusHistory().add(history);

        ServiceOrder saved = orderRepository.save(order);

        auditService.record(actorId, "SERVICE_STATUS_CHANGED", "SERVICE_ORDER", saved.getId(), Map.of(
                "orderNumber", saved.getOrderNumber(),
                "oldStatus", current,
                "newStatus", target
        ), ipAddress);

        return toDetail(saved);
    }

    @Transactional
    public ServiceOrderDto consumeMaterial(UUID orderId, UUID materialId, BigDecimal quantity, UUID actorId) {
        ServiceOrder order = findOrder(orderId);
        ensureActiveOrder(order);

        ServiceMaterial material = materialRepository.findByIdAndServiceOrderId(materialId, orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Material de la orden", materialId));

        BigDecimal newConsumed = material.getConsumedQuantity().add(quantity);
        if (newConsumed.compareTo(material.getPlannedQuantity()) > 0) {
            throw new BusinessRuleException("CONSUMED_EXCEEDS_PLANNED",
                    String.format("El consumo (%s) excede la cantidad planificada (%s). Actualice el plan antes de consumir.",
                            newConsumed, material.getPlannedQuantity()));
        }

        stockService.deductStock(
                material.getVariantId(),
                null,
                quantity,
                "SERVICE_CONSUMPTION",
                "SERVICE_ORDER",
                order.getId(),
                material.getId(),
                actorId,
                "Consumo de material orden " + order.getOrderNumber());

        material.setConsumedQuantity(newConsumed);
        materialRepository.save(material);
        order.setUpdatedAt(OffsetDateTime.now());
        orderRepository.save(order);

        return toDetail(order);
    }

    @Transactional
    public ServiceOrderDto returnMaterial(UUID orderId, UUID materialId, BigDecimal quantity, UUID actorId) {
        ServiceOrder order = findOrder(orderId);

        ServiceMaterial material = materialRepository.findByIdAndServiceOrderId(materialId, orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Material de la orden", materialId));

        if (quantity.compareTo(material.getConsumedQuantity()) > 0) {
            throw new BusinessRuleException("RETURN_EXCEEDS_CONSUMED",
                    String.format("La cantidad a devolver (%s) es mayor al material consumido (%s)",
                            quantity, material.getConsumedQuantity()));
        }

        stockService.addStock(
                material.getVariantId(),
                null,
                quantity,
                null,
                "SERVICE_RETURN",
                "SERVICE_ORDER",
                order.getId(),
                material.getId(),
                actorId,
                "Devolución de material orden " + order.getOrderNumber());

        material.setConsumedQuantity(material.getConsumedQuantity().subtract(quantity));
        materialRepository.save(material);
        order.setUpdatedAt(OffsetDateTime.now());
        orderRepository.save(order);

        return toDetail(order);
    }

    @Transactional
    public ServiceOrderDto.AdvanceDto addAdvance(UUID orderId, CreateAdvanceRequest request, UUID actorId, String ipAddress) {
        ServiceOrder order = findOrder(orderId);
        ensureActiveOrder(order);

        String paymentMethod = request.getPaymentMethod().trim().toUpperCase();
        if (!ADVANCE_PAYMENT_METHODS.contains(paymentMethod)) {
            throw new BusinessRuleException("INVALID_PAYMENT_METHOD", "Medio de pago no válido: " + request.getPaymentMethod());
        }

        CashSession session = cashSessionRepository.findById(request.getCashSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Sesión de caja", request.getCashSessionId()));
        if (!"OPEN".equals(session.getStatus())) {
            throw new BusinessRuleException("CASH_SESSION_CLOSED", "La sesión de caja está cerrada");
        }

        ServiceAdvance advance = new ServiceAdvance();
        advance.setServiceOrder(order);
        advance.setCashSessionId(session.getId());
        advance.setAmount(request.getAmount());
        advance.setAppliedAmount(BigDecimal.ZERO);
        advance.setPaymentMethod(paymentMethod);
        advance.setReference(request.getReference());
        order.getAdvances().add(advance);
        order.setUpdatedAt(OffsetDateTime.now());
        orderRepository.save(order);

        // El anticipo en efectivo se refleja físicamente en la caja
        if ("CASH".equals(paymentMethod)) {
            CashMovement movement = new CashMovement();
            movement.setSessionId(session.getId());
            movement.setMovementType("ADVANCE_PAYMENT");
            movement.setAmount(request.getAmount());
            movement.setSourceType("SERVICE_ORDER");
            movement.setSourceId(order.getId());
            movement.setActorId(actorId);
            movement.setReason("Anticipo de orden " + order.getOrderNumber());
            cashMovementRepository.save(movement);
        }

        auditService.record(actorId, "ADVANCE_RECEIVED", "SERVICE_ORDER", order.getId(), Map.of(
                "orderNumber", order.getOrderNumber(),
                "amount", request.getAmount(),
                "paymentMethod", paymentMethod
        ), ipAddress);

        return toAdvanceDto(advance);
    }

    @Transactional(readOnly = true)
    public List<ServiceOrderDto.AdvanceDto> listAdvances(UUID orderId) {
        findOrder(orderId);
        return advanceRepository.findByServiceOrderIdOrderByCreatedAtAsc(orderId).stream()
                .map(this::toAdvanceDto)
                .toList();
    }

    /** Cobra el saldo de la orden creando una venta e aplicando anticipos una sola vez. */
    @Transactional
    public SaleResponseDto charge(UUID orderId, ChargeServiceOrderRequest request, UUID idempotencyKey,
                                  UUID actorId, String ipAddress) {
        ServiceOrder order = findOrder(orderId);
        if ("CANCELLED".equals(order.getStatus())) {
            throw new BusinessRuleException("CHARGE_NOT_ALLOWED",
                    "No se puede cobrar una orden cancelada. Estado actual: " + order.getStatus());
        }

        ServiceOrderChargeCommand command = new ServiceOrderChargeCommand();
        command.setServiceOrderId(order.getId());
        command.setOrderNumber(order.getOrderNumber());
        command.setCustomerId(order.getCustomerId());
        command.setCashSessionId(request.getCashSessionId());
        command.setLaborAmount(order.getLaborAmount());
        for (ServiceOrderItem item : order.getItems()) {
            command.getLines().add(new ServiceOrderChargeCommand.ChargeLine(
                    item.getDescription(), item.getQuantity(), item.getUnitPrice()));
        }
        for (ServiceAdvance advance : order.getAdvances()) {
            command.getAdvances().add(new ServiceOrderChargeCommand.AdvanceInfo(
                    advance.getId(), advance.getAmount(), advance.getAppliedAmount()));
        }
        for (ChargeServiceOrderRequest.PaymentItem payment : request.getPayments()) {
            ServiceOrderChargeCommand.ChargePayment chargePayment = new ServiceOrderChargeCommand.ChargePayment();
            chargePayment.setMethod(payment.getMethod());
            chargePayment.setAmount(payment.getAmount());
            chargePayment.setReference(payment.getReference());
            command.getPayments().add(chargePayment);
        }

        ServiceChargeResultDto result = saleChargeService.charge(command, idempotencyKey, actorId);

        if (!result.isReplay()) {
            Map<UUID, ServiceAdvance> advancesById = new HashMap<>();
            for (ServiceAdvance advance : order.getAdvances()) {
                advancesById.put(advance.getId(), advance);
            }
            for (ServiceChargeResultDto.AppliedAdvanceDto allocation : result.getAppliedAdvances()) {
                ServiceAdvance advance = advancesById.get(allocation.getAdvanceId());
                if (advance != null) {
                    advance.setAppliedAmount(advance.getAppliedAmount().add(allocation.getAppliedAmount()));
                }
            }

            String current = order.getStatus();
            // El cobro deja la orden lista para entrega; una orden ya entregada conserva su estado
            String nextStatus = "DELIVERED".equals(current) ? current : "READY_FOR_DELIVERY";
            order.setStatus(nextStatus);
            order.setUpdatedAt(OffsetDateTime.now());

            ServiceStatusHistory history = new ServiceStatusHistory();
            history.setServiceOrder(order);
            history.setOldStatus(current);
            history.setNewStatus(nextStatus);
            history.setActorId(actorId);
            history.setNotes("Saldo cobrado con venta " + result.getSale().getSaleNumber());
            order.getStatusHistory().add(history);

            orderRepository.save(order);

            auditService.record(actorId, "SERVICE_CHARGED", "SERVICE_ORDER", order.getId(), Map.of(
                    "orderNumber", order.getOrderNumber(),
                    "saleNumber", result.getSale().getSaleNumber(),
                    "saleTotal", result.getSale().getGrandTotal(),
                    "appliedAdvances", result.getAppliedAdvances().size()
            ), ipAddress);
        }

        return result.getSale();
    }

    @Transactional
    public ServiceAttachmentDto addAttachment(UUID orderId, AttachMediaRequest request) {
        findOrder(orderId);

        MediaFile media = mediaFileRepository.findById(request.getMediaId())
                .orElseThrow(() -> new ResourceNotFoundException("Archivo", request.getMediaId()));

        ServiceAttachmentId attachmentId = new ServiceAttachmentId(orderId, media.getId());
        if (attachmentRepository.existsById(attachmentId)) {
            throw new BusinessRuleException("ATTACHMENT_ALREADY_LINKED", "El archivo ya está asociado a la orden");
        }

        ServiceAttachment attachment = new ServiceAttachment();
        attachment.setServiceOrderId(orderId);
        attachment.setMediaId(media.getId());
        attachment.setAttachmentType(request.getAttachmentType() == null || request.getAttachmentType().isBlank()
                ? "DESIGN" : request.getAttachmentType().trim().toUpperCase());
        attachmentRepository.save(attachment);

        return toAttachmentDto(attachment, media);
    }

    @Transactional
    public void removeAttachment(UUID orderId, UUID mediaId) {
        findOrder(orderId);
        ServiceAttachmentId attachmentId = new ServiceAttachmentId(orderId, mediaId);
        ServiceAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Adjunto de la orden", mediaId));
        attachmentRepository.delete(attachment);
    }

    @Transactional(readOnly = true)
    public List<ServiceAttachmentDto> listAttachments(UUID orderId) {
        findOrder(orderId);
        List<ServiceAttachment> attachments = attachmentRepository.findByServiceOrderId(orderId);
        Map<UUID, MediaFile> medias = loadMedias(attachments);
        return attachments.stream()
                .map(attachment -> toAttachmentDto(attachment, medias.get(attachment.getMediaId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceAttachmentDto> listImages(UUID orderId) {
        return listAttachments(orderId).stream()
                .filter(dto -> dto.getContentType() != null && dto.getContentType().startsWith("image/"))
                .toList();
    }

    /** Órdenes programadas entre hoy -7 y hoy +7 días agrupadas por fecha (America/Guayaquil). */
    @Transactional(readOnly = true)
    public List<AgendaDayDto> agenda() {
        LocalDate today = LocalDate.now(ECUADOR);
        OffsetDateTime from = today.minusDays(7).atStartOfDay(ECUADOR).toOffsetDateTime();
        OffsetDateTime to = today.plusDays(8).atStartOfDay(ECUADOR).toOffsetDateTime();

        Map<LocalDate, AgendaDayDto> byDate = new TreeMap<>();
        for (ServiceOrder order : orderRepository.findScheduledBetween(from, to)) {
            LocalDate date = order.getScheduledAt().atZoneSameInstant(ECUADOR).toLocalDate();
            byDate.computeIfAbsent(date, AgendaDayDto::new).getOrders().add(toSummary(order));
        }
        return new ArrayList<>(byDate.values());
    }

    private ServiceOrder findOrder(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de servicio", id));
    }

    /**
     * estimated_total = mano de obra + ítems + IVA resuelto en tax_rules (la tabla no guarda impuestos).
     * El impuesto se calcula línea por línea, igual que la venta de cobro, para que
     * el total estimado y el total de la venta coincidan exactamente al céntimo.
     */
    private void recalculateEstimatedTotal(ServiceOrder order) {
        TaxRule taxRule = taxService.resolveDefault();
        BigDecimal base = order.getLaborAmount() != null ? order.getLaborAmount() : BigDecimal.ZERO;
        BigDecimal tax = taxService.taxAmount(base, taxRule.getRate());
        for (ServiceOrderItem item : order.getItems()) {
            base = base.add(item.getLineTotal());
            tax = tax.add(taxService.taxAmount(item.getLineTotal(), taxRule.getRate()));
        }
        order.setEstimatedTotal(base.add(tax).setScale(2, RoundingMode.HALF_UP));
    }

    private void ensureActiveOrder(ServiceOrder order) {
        if ("DELIVERED".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) {
            throw new BusinessRuleException("ORDER_NOT_ACTIVE",
                    "La orden está " + order.getStatus() + " y no admite cambios de materiales");
        }
    }

    private boolean isEditableStatus(String status) {
        return "QUOTED".equals(status) || "CONFIRMED".equals(status) || "IN_PROGRESS".equals(status);
    }

    private Map<String, List<String>> allowedTransitions() {
        Map<String, List<String>> transitions = new HashMap<>();
        transitions.put("QUOTED", List.of("CONFIRMED", "CANCELLED"));
        transitions.put("CONFIRMED", List.of("IN_PROGRESS", "CANCELLED"));
        transitions.put("IN_PROGRESS", List.of("READY_FOR_DELIVERY", "CANCELLED"));
        transitions.put("READY_FOR_DELIVERY", List.of("DELIVERED", "CANCELLED"));
        transitions.put("DELIVERED", List.of());
        transitions.put("CANCELLED", List.of());
        return transitions;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ServiceOrderDto.AdvanceDto toAdvanceDto(ServiceAdvance advance) {
        ServiceOrderDto.AdvanceDto dto = new ServiceOrderDto.AdvanceDto();
        dto.setId(advance.getId());
        dto.setCashSessionId(advance.getCashSessionId());
        dto.setAmount(advance.getAmount());
        dto.setAppliedAmount(advance.getAppliedAmount());
        dto.setPaymentMethod(advance.getPaymentMethod());
        dto.setReference(advance.getReference());
        dto.setCreatedAt(advance.getCreatedAt());
        return dto;
    }

    private ServiceOrderSummaryDto toSummary(ServiceOrder order) {
        BigDecimal advancesTotal = order.getAdvances().stream()
                .map(ServiceAdvance::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ServiceOrderSummaryDto dto = new ServiceOrderSummaryDto();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setCustomerId(order.getCustomerId());
        dto.setVehicleId(order.getVehicleId());
        dto.setServiceType(order.getServiceType());
        dto.setDescription(order.getDescription());
        dto.setStatus(order.getStatus());
        dto.setAssignedUserId(order.getAssignedUserId());
        dto.setLaborAmount(order.getLaborAmount());
        dto.setEstimatedTotal(order.getEstimatedTotal());
        dto.setRemainingBalance(order.getEstimatedTotal().subtract(advancesTotal));
        dto.setScheduledAt(order.getScheduledAt());
        dto.setCreatedAt(order.getCreatedAt());
        return dto;
    }

    private ServiceOrderDto toDetail(ServiceOrder order) {
        ServiceOrderDto dto = new ServiceOrderDto();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setCustomerId(order.getCustomerId());
        dto.setVehicleId(order.getVehicleId());
        dto.setServiceType(order.getServiceType());
        dto.setDescription(order.getDescription());
        dto.setDimensions(order.getDimensions());
        dto.setStatus(order.getStatus());
        dto.setAssignedUserId(order.getAssignedUserId());
        dto.setLaborAmount(order.getLaborAmount());
        dto.setEstimatedTotal(order.getEstimatedTotal());
        dto.setScheduledAt(order.getScheduledAt());
        dto.setCompletedAt(order.getCompletedAt());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());

        for (ServiceOrderItem item : order.getItems()) {
            ServiceOrderDto.ItemDto itemDto = new ServiceOrderDto.ItemDto();
            itemDto.setId(item.getId());
            itemDto.setDescription(item.getDescription());
            itemDto.setQuantity(item.getQuantity());
            itemDto.setUnitPrice(item.getUnitPrice());
            itemDto.setLineTotal(item.getLineTotal());
            dto.getItems().add(itemDto);
        }

        for (ServiceMaterial material : order.getMaterials()) {
            ServiceOrderDto.MaterialDto materialDto = new ServiceOrderDto.MaterialDto();
            materialDto.setId(material.getId());
            materialDto.setVariantId(material.getVariantId());
            materialDto.setPlannedQuantity(material.getPlannedQuantity());
            materialDto.setConsumedQuantity(material.getConsumedQuantity());
            dto.getMaterials().add(materialDto);
        }

        List<ServiceAttachment> attachments = attachmentRepository.findByServiceOrderId(order.getId());
        Map<UUID, MediaFile> medias = loadMedias(attachments);
        for (ServiceAttachment attachment : attachments) {
            dto.getAttachments().add(toAttachmentDto(attachment, medias.get(attachment.getMediaId())));
        }

        for (ServiceStatusHistory history : order.getStatusHistory()) {
            ServiceOrderDto.StatusHistoryDto historyDto = new ServiceOrderDto.StatusHistoryDto();
            historyDto.setId(history.getId());
            historyDto.setOldStatus(history.getOldStatus());
            historyDto.setNewStatus(history.getNewStatus());
            historyDto.setActorId(history.getActorId());
            historyDto.setChangedAt(history.getChangedAt());
            historyDto.setNotes(history.getNotes());
            dto.getStatusHistory().add(historyDto);
        }

        BigDecimal advancesTotal = BigDecimal.ZERO;
        for (ServiceAdvance advance : order.getAdvances()) {
            dto.getAdvances().add(toAdvanceDto(advance));
            advancesTotal = advancesTotal.add(advance.getAmount());
        }
        dto.setAdvancesTotal(advancesTotal);
        dto.setRemainingBalance(order.getEstimatedTotal().subtract(advancesTotal));

        return dto;
    }

    private Map<UUID, MediaFile> loadMedias(List<ServiceAttachment> attachments) {
        if (attachments.isEmpty()) {
            return Map.of();
        }
        List<UUID> mediaIds = attachments.stream().map(ServiceAttachment::getMediaId).distinct().toList();
        Map<UUID, MediaFile> medias = new HashMap<>();
        for (MediaFile media : mediaFileRepository.findAllById(mediaIds)) {
            medias.put(media.getId(), media);
        }
        return medias;
    }

    private ServiceAttachmentDto toAttachmentDto(ServiceAttachment attachment, MediaFile media) {
        ServiceAttachmentDto dto = new ServiceAttachmentDto();
        dto.setMediaId(attachment.getMediaId());
        dto.setAttachmentType(attachment.getAttachmentType());
        if (media != null) {
            dto.setStorageKey(media.getStorageKey());
            dto.setOriginalName(media.getOriginalName());
            dto.setContentType(media.getContentType());
            dto.setSizeBytes(media.getSizeBytes());
        }
        return dto;
    }
}
