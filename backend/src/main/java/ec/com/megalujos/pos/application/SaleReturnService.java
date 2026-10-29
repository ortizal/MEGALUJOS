package ec.com.megalujos.pos.application;

import ec.com.megalujos.catalog.domain.Product;
import ec.com.megalujos.catalog.domain.ProductVariant;
import ec.com.megalujos.catalog.domain.ProductVariantRepository;
import ec.com.megalujos.cash.domain.CashMovement;
import ec.com.megalujos.cash.domain.CashMovementRepository;
import ec.com.megalujos.common.audit.AuditService;
import ec.com.megalujos.common.exception.BusinessRuleException;
import ec.com.megalujos.common.exception.ResourceNotFoundException;
import ec.com.megalujos.common.web.PageResponse;
import ec.com.megalujos.inventory.application.StockService;
import ec.com.megalujos.pos.domain.*;
import ec.com.megalujos.pos.dto.CreateReturnRequest;
import ec.com.megalujos.pos.dto.SaleDetailDto;
import ec.com.megalujos.pos.dto.SaleResponseDto;
import ec.com.megalujos.pos.dto.SaleReturnDto;
import ec.com.megalujos.pos.dto.SaleSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Devoluciones de venta e historial de ventas.
 * La venta original nunca se borra: se registra una devolución compensatoria
 * y el estado de la venta pasa a RETURNED o PARTIALLY_RETURNED.
 */
@Service
public class SaleReturnService {

    private static final List<String> RETURN_PAYMENT_METHODS = List.of("CASH", "TRANSFER", "CARD", "OTHER");

    private final SaleRepository saleRepository;
    private final SaleSearchRepository saleSearchRepository;
    private final SaleReturnRepository saleReturnRepository;
    private final SaleReturnItemRepository saleReturnItemRepository;
    private final CashMovementRepository cashMovementRepository;
    private final ProductVariantRepository variantRepository;
    private final StockService stockService;
    private final AuditService auditService;

    public SaleReturnService(SaleRepository saleRepository,
                             SaleSearchRepository saleSearchRepository,
                             SaleReturnRepository saleReturnRepository,
                             SaleReturnItemRepository saleReturnItemRepository,
                             CashMovementRepository cashMovementRepository,
                             ProductVariantRepository variantRepository,
                             StockService stockService,
                             AuditService auditService) {
        this.saleRepository = saleRepository;
        this.saleSearchRepository = saleSearchRepository;
        this.saleReturnRepository = saleReturnRepository;
        this.saleReturnItemRepository = saleReturnItemRepository;
        this.cashMovementRepository = cashMovementRepository;
        this.variantRepository = variantRepository;
        this.stockService = stockService;
        this.auditService = auditService;
    }

    @Transactional
    public SaleReturnDto createReturn(UUID saleId, CreateReturnRequest request, UUID actorId, String ipAddress) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Venta", saleId));

        if (!"CONFIRMED".equals(sale.getStatus()) && !"PARTIALLY_RETURNED".equals(sale.getStatus())) {
            throw new BusinessRuleException("SALE_NOT_RETURNABLE",
                    "Solo se pueden devolver ventas CONFIRMED o PARTIALLY_RETURNED. Estado actual: " + sale.getStatus());
        }

        String paymentMethod = request.getPaymentMethod() == null || request.getPaymentMethod().isBlank()
                ? "CASH" : request.getPaymentMethod().trim().toUpperCase();
        if (!RETURN_PAYMENT_METHODS.contains(paymentMethod)) {
            throw new BusinessRuleException("INVALID_PAYMENT_METHOD", "Medio de pago de devolución no válido: " + paymentMethod);
        }

        Map<UUID, SaleItem> itemById = new HashMap<>();
        for (SaleItem item : sale.getItems()) {
            itemById.put(item.getId(), item);
        }

        SaleReturn saleReturn = new SaleReturn();
        String datePrefix = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        saleReturn.setReturnNumber("DEV-" + datePrefix + "-" + randomSuffix);
        saleReturn.setSaleId(sale.getId());
        saleReturn.setCashSessionId(sale.getCashSessionId());
        saleReturn.setReason(request.getReason());
        saleReturn.setAuthorizedBy(actorId);
        saleReturn.setPaymentMethod(paymentMethod);

        BigDecimal totalReturned = BigDecimal.ZERO;
        Map<UUID, BigDecimal> requestedInFlight = new HashMap<>();

        for (CreateReturnRequest.ReturnItemRequest itemReq : request.getItems()) {
            SaleItem item = itemById.get(itemReq.getSaleItemId());
            if (item == null) {
                throw new BusinessRuleException("ITEM_NOT_IN_SALE",
                        "El ítem " + itemReq.getSaleItemId() + " no pertenece a la venta " + sale.getSaleNumber());
            }

            BigDecimal alreadyReturned = saleReturnItemRepository.sumReturnedQuantityBySaleItem(item.getId());
            BigDecimal pendingHere = requestedInFlight.getOrDefault(item.getId(), BigDecimal.ZERO);
            BigDecimal maxReturnable = item.getQuantity().subtract(alreadyReturned).subtract(pendingHere);
            if (itemReq.getQuantity().compareTo(maxReturnable) > 0) {
                throw new BusinessRuleException("QUANTITY_EXCEEDS_RETURNABLE",
                        String.format("Cantidad a devolver (%s) mayor a la disponible por devolver (%s) del ítem %s",
                                itemReq.getQuantity(), maxReturnable, item.getDescription()));
            }
            requestedInFlight.merge(item.getId(), itemReq.getQuantity(), BigDecimal::add);

            BigDecimal returnAmount = computeReturnAmount(item, itemReq.getQuantity());

            SaleReturnItem returnItem = new SaleReturnItem();
            returnItem.setSaleReturn(saleReturn);
            returnItem.setSaleItemId(item.getId());
            returnItem.setQuantity(itemReq.getQuantity());
            returnItem.setReturnAmount(returnAmount);
            returnItem.setRestock(itemReq.getRestock() == null || itemReq.getRestock());
            saleReturn.getItems().add(returnItem);

            totalReturned = totalReturned.add(returnAmount);
        }

        if (totalReturned.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("INVALID_RETURN_AMOUNT", "El total devuelto debe ser mayor a cero");
        }
        saleReturn.setTotalReturned(totalReturned);
        SaleReturn saved = saleReturnRepository.save(saleReturn);

        // Reintegro de inventario (solo líneas con variante y marcadas para restock)
        for (SaleReturnItem returnItem : saved.getItems()) {
            SaleItem item = itemById.get(returnItem.getSaleItemId());
            if (returnItem.isRestock() && item.getVariantId() != null) {
                stockService.addStock(
                        item.getVariantId(),
                        null,
                        returnItem.getQuantity(),
                        null,
                        "SALE_RETURN",
                        "SALE_RETURN",
                        saved.getId(),
                        returnItem.getId(),
                        actorId,
                        "Devolución " + saved.getReturnNumber()
                );
            }
        }

        // Movimiento de caja solo cuando el dinero vuelve en efectivo
        if ("CASH".equals(paymentMethod)) {
            CashMovement movement = new CashMovement();
            movement.setSessionId(sale.getCashSessionId());
            movement.setMovementType("REFUND");
            movement.setAmount(totalReturned);
            movement.setSourceType("SALE_RETURN");
            movement.setSourceId(saved.getId());
            movement.setActorId(actorId);
            movement.setReason("Devolución " + saved.getReturnNumber() + " venta #" + sale.getSaleNumber());
            cashMovementRepository.save(movement);
        }

        BigDecimal totalReturnedAll = saleReturnRepository.sumTotalReturnedBySaleId(sale.getId());
        sale.setStatus(totalReturnedAll.compareTo(sale.getGrandTotal()) >= 0 ? "RETURNED" : "PARTIALLY_RETURNED");

        String note = "Devolución " + saved.getReturnNumber() + " (" + paymentMethod + ") por " + totalReturned;
        sale.setNotes(sale.getNotes() != null ? sale.getNotes() + "\n" + note : note);
        saleRepository.save(sale);

        auditService.record(actorId, "SALE_RETURNED", "SALE", sale.getId(), Map.of(
                "returnNumber", saved.getReturnNumber(),
                "totalReturned", totalReturned,
                "paymentMethod", paymentMethod,
                "restock", request.getItems().stream().anyMatch(i -> i.getRestock() == null || i.getRestock())
        ), ipAddress);

        return toReturnDto(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<SaleSummaryDto> listSales(OffsetDateTime dateFrom, OffsetDateTime dateTo, String status,
                                                  UUID customerId, String q, UUID cashSessionId, Pageable pageable) {
        Pageable sorted = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Sale> page = saleSearchRepository.search(dateFrom, dateTo, status, customerId, cashSessionId, q, sorted);
        Map<UUID, BigDecimal> returnedTotals = loadReturnedTotals(page.getContent());

        return PageResponse.from(page, sale -> toSummary(sale, returnedTotals.getOrDefault(sale.getId(), BigDecimal.ZERO)));
    }

    @Transactional(readOnly = true)
    public SaleDetailDto getSaleDetail(UUID saleId) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Venta", saleId));

        SaleDetailDto dto = new SaleDetailDto();
        dto.setSaleId(sale.getId());
        dto.setSaleNumber(sale.getSaleNumber());
        dto.setStatus(sale.getStatus());
        dto.setSubtotal(sale.getSubtotal());
        dto.setDiscountTotal(sale.getDiscountTotal());
        dto.setTaxTotal(sale.getTaxTotal());
        dto.setGrandTotal(sale.getGrandTotal());
        dto.setIdempotencyKey(sale.getIdempotencyKey());
        dto.setCreatedAt(sale.getCreatedAt());
        dto.setInvoiceStatus("CONFIRMED");
        dto.setCustomerId(sale.getCustomerId());
        dto.setCashSessionId(sale.getCashSessionId());

        List<SaleResponseDto.SaleItemResponseDto> itemDtos = new ArrayList<>();
        for (SaleItem item : sale.getItems()) {
            SaleResponseDto.SaleItemResponseDto itemDto = new SaleResponseDto.SaleItemResponseDto();
            itemDto.setId(item.getId());
            itemDto.setDescription(item.getDescription());
            itemDto.setQuantity(item.getQuantity());
            itemDto.setUnitPrice(item.getUnitPrice());
            itemDto.setDiscount(item.getDiscount());
            itemDto.setTaxAmount(item.getTaxAmount());
            itemDto.setLineTotal(item.getLineTotal());
            itemDtos.add(itemDto);
        }
        dto.setItems(itemDtos);

        List<SaleResponseDto.SalePaymentResponseDto> paymentDtos = new ArrayList<>();
        for (SalePayment payment : sale.getPayments()) {
            paymentDtos.add(new SaleResponseDto.SalePaymentResponseDto(
                    payment.getMethod(), payment.getAmount(), payment.getReference()));
        }
        dto.setPayments(paymentDtos);

        List<SaleReturnDto> returnDtos = new ArrayList<>();
        BigDecimal totalReturned = BigDecimal.ZERO;
        for (SaleReturn saleReturn : saleReturnRepository.findBySaleIdOrderByCreatedAtDesc(sale.getId())) {
            returnDtos.add(toReturnDto(saleReturn));
            totalReturned = totalReturned.add(saleReturn.getTotalReturned());
        }
        dto.setReturns(returnDtos);
        dto.setTotalReturned(totalReturned);

        return dto;
    }

    /** Devoluciones registradas sobre una venta, de la más reciente a la más antigua. */
    @Transactional(readOnly = true)
    public List<SaleReturnDto> listReturns(UUID saleId) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Venta", saleId));
        return saleReturnRepository.findBySaleIdOrderByCreatedAtDesc(sale.getId()).stream()
                .map(this::toReturnDto)
                .toList();
    }

    /** Anula una venta sin facturas electrónicas y sin devoluciones. */
    @Transactional
    public SaleDetailDto voidSale(UUID saleId, UUID actorId, String ipAddress) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Venta", saleId));

        if (!"DRAFT".equals(sale.getStatus()) && !"CONFIRMED".equals(sale.getStatus())) {
            throw new BusinessRuleException("SALE_NOT_VOIDABLE",
                    "Solo se pueden anular ventas DRAFT o CONFIRMED. Estado actual: " + sale.getStatus());
        }
        if (saleSearchRepository.countElectronicDocumentsBySale(saleId) > 0) {
            throw new BusinessRuleException("SALE_HAS_ELECTRONIC_DOCUMENTS",
                    "La venta tiene comprobantes electrónicos asociados; use una nota de crédito");
        }
        if (saleReturnRepository.sumTotalReturnedBySaleId(saleId).compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessRuleException("SALE_HAS_RETURNS", "La venta tiene devoluciones asociadas y no puede anularse");
        }

        // Las ventas confirmadas descontaron stock: se reintegra con trazabilidad en kardex
        if ("CONFIRMED".equals(sale.getStatus())) {
            for (SaleItem item : sale.getItems()) {
                if (item.getVariantId() == null) {
                    continue;
                }
                ProductVariant variant = variantRepository.findById(item.getVariantId()).orElse(null);
                if (variant == null) {
                    continue;
                }
                Product product = variant.getProduct();
                if (product.isTrackStock() && !"SERVICE".equalsIgnoreCase(product.getProductType())) {
                    stockService.addStock(
                            item.getVariantId(), null, item.getQuantity(), null,
                            "SALE_VOID", "POS_SALE", sale.getId(), item.getId(), actorId,
                            "Anulación de venta #" + sale.getSaleNumber());
                }
            }
        }

        sale.setStatus("VOIDED");
        saleRepository.save(sale);

        auditService.record(actorId, "SALE_VOIDED", "SALE", sale.getId(),
                Map.of("saleNumber", sale.getSaleNumber()), ipAddress);

        return getSaleDetail(saleId);
    }

    /** Devolución proporcional: (unit_price * qty - parte del descuento) * (1 + tasa histórica del ítem). */
    private BigDecimal computeReturnAmount(SaleItem item, BigDecimal quantity) {
        BigDecimal base = item.getUnitPrice().multiply(quantity).setScale(4, RoundingMode.HALF_UP);
        BigDecimal discountShare = BigDecimal.ZERO;
        if (item.getDiscount() != null && item.getDiscount().compareTo(BigDecimal.ZERO) > 0
                && item.getQuantity() != null && item.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
            discountShare = item.getDiscount().multiply(quantity)
                    .divide(item.getQuantity(), 4, RoundingMode.HALF_UP);
        }
        BigDecimal net = base.subtract(discountShare);
        if (net.compareTo(BigDecimal.ZERO) < 0) {
            net = BigDecimal.ZERO;
        }
        BigDecimal rate = item.getTaxRate() != null ? item.getTaxRate() : BigDecimal.ZERO;
        return net.multiply(BigDecimal.ONE.add(rate)).setScale(2, RoundingMode.HALF_UP);
    }

    private Map<UUID, BigDecimal> loadReturnedTotals(List<Sale> sales) {
        Map<UUID, BigDecimal> result = new HashMap<>();
        if (sales.isEmpty()) {
            return result;
        }
        List<UUID> ids = sales.stream().map(Sale::getId).toList();
        for (Object[] row : saleReturnRepository.sumTotalReturnedBySaleIds(ids)) {
            result.put((UUID) row[0], (BigDecimal) row[1]);
        }
        return result;
    }

    private SaleSummaryDto toSummary(Sale sale, BigDecimal totalReturned) {
        SaleSummaryDto dto = new SaleSummaryDto();
        dto.setSaleId(sale.getId());
        dto.setSaleNumber(sale.getSaleNumber());
        dto.setCustomerId(sale.getCustomerId());
        dto.setCashSessionId(sale.getCashSessionId());
        dto.setStatus(sale.getStatus());
        dto.setSubtotal(sale.getSubtotal());
        dto.setDiscountTotal(sale.getDiscountTotal());
        dto.setTaxTotal(sale.getTaxTotal());
        dto.setGrandTotal(sale.getGrandTotal());
        dto.setTotalReturned(totalReturned);
        dto.setCreatedAt(sale.getCreatedAt());
        return dto;
    }

    private SaleReturnDto toReturnDto(SaleReturn saleReturn) {
        SaleReturnDto dto = new SaleReturnDto();
        dto.setId(saleReturn.getId());
        dto.setReturnNumber(saleReturn.getReturnNumber());
        dto.setSaleId(saleReturn.getSaleId());
        dto.setCashSessionId(saleReturn.getCashSessionId());
        dto.setTotalReturned(saleReturn.getTotalReturned());
        dto.setReason(saleReturn.getReason());
        dto.setPaymentMethod(saleReturn.getPaymentMethod());
        dto.setAuthorizedBy(saleReturn.getAuthorizedBy());
        dto.setCreatedAt(saleReturn.getCreatedAt());

        List<SaleReturnDto.SaleReturnItemDto> itemDtos = new ArrayList<>();
        for (SaleReturnItem item : saleReturn.getItems()) {
            SaleReturnDto.SaleReturnItemDto itemDto = new SaleReturnDto.SaleReturnItemDto();
            itemDto.setId(item.getId());
            itemDto.setSaleItemId(item.getSaleItemId());
            itemDto.setQuantity(item.getQuantity());
            itemDto.setReturnAmount(item.getReturnAmount());
            itemDto.setRestock(item.isRestock());
            itemDtos.add(itemDto);
        }
        dto.setItems(itemDtos);
        return dto;
    }
}
