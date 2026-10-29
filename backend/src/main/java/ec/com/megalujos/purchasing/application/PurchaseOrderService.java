package ec.com.megalujos.purchasing.application;

import ec.com.megalujos.catalog.domain.ProductVariant;
import ec.com.megalujos.catalog.domain.ProductVariantRepository;
import ec.com.megalujos.common.exception.BusinessRuleException;
import ec.com.megalujos.common.exception.ResourceNotFoundException;
import ec.com.megalujos.common.tax.TaxRule;
import ec.com.megalujos.common.tax.TaxService;
import ec.com.megalujos.purchasing.domain.*;
import ec.com.megalujos.purchasing.dto.CreatePurchaseOrderRequest;
import ec.com.megalujos.purchasing.dto.PurchaseOrderResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class PurchaseOrderService {

    private final PurchaseOrderRepository orderRepository;
    private final SupplierRepository supplierRepository;
    private final ProductVariantRepository variantRepository;
    private final GoodsReceiptItemRepository goodsReceiptItemRepository;
    private final TaxService taxService;

    public PurchaseOrderService(PurchaseOrderRepository orderRepository,
                                SupplierRepository supplierRepository,
                                ProductVariantRepository variantRepository,
                                GoodsReceiptItemRepository goodsReceiptItemRepository,
                                TaxService taxService) {
        this.orderRepository = orderRepository;
        this.supplierRepository = supplierRepository;
        this.variantRepository = variantRepository;
        this.goodsReceiptItemRepository = goodsReceiptItemRepository;
        this.taxService = taxService;
    }

    @Transactional(readOnly = true)
    public Page<PurchaseOrderResponseDto> list(String status, String q, Pageable pageable) {
        String normalizedStatus = normalize(status);
        if (normalizedStatus != null) {
            validateStatusValue(normalizedStatus);
        }
        return orderRepository.search(normalizedStatus, normalize(q), pageable)
                .map(order -> map(order, null, false));
    }

    @Transactional
    public PurchaseOrderResponseDto create(CreatePurchaseOrderRequest request, UUID actorId) {
        requireActiveSupplier(request.getSupplierId());

        PurchaseOrder order = new PurchaseOrder();
        order.setOrderNumber(nextNumber("OC-"));
        order.setStatus("DRAFT");
        order.setSupplierId(request.getSupplierId());
        order.setNotes(normalize(request.getNotes()));
        rebuildItems(order, request);
        applyTotals(order);

        PurchaseOrder saved = orderRepository.save(order);
        return map(saved, null, false);
    }

    @Transactional
    public PurchaseOrderResponseDto update(UUID id, CreatePurchaseOrderRequest request, UUID actorId) {
        PurchaseOrder order = requireOrder(id);
        if (!"DRAFT".equals(order.getStatus())) {
            throw new BusinessRuleException("ORDER_NOT_DRAFT", "Solo las órdenes en estado DRAFT pueden modificarse");
        }
        requireActiveSupplier(request.getSupplierId());

        order.setSupplierId(request.getSupplierId());
        order.setNotes(normalize(request.getNotes()));
        rebuildItems(order, request);
        applyTotals(order);

        PurchaseOrder saved = orderRepository.save(order);
        return map(saved, null, false);
    }

    @Transactional
    public PurchaseOrderResponseDto markOrdered(UUID id) {
        PurchaseOrder order = requireOrder(id);
        if (!"DRAFT".equals(order.getStatus())) {
            throw new BusinessRuleException("ORDER_NOT_DRAFT", "Solo las órdenes en estado DRAFT pueden enviarse a proveedor");
        }
        order.setStatus("ORDERED");
        return map(orderRepository.save(order), null, false);
    }

    @Transactional(readOnly = true)
    public PurchaseOrderResponseDto get(UUID id) {
        PurchaseOrder order = requireOrder(id);
        Map<UUID, BigDecimal> received = new HashMap<>();
        for (ReceivedQuantityRow row : goodsReceiptItemRepository.sumReceivedByOrder(id)) {
            received.put(row.getOrderItemId(), row.getQuantity());
        }
        return map(order, received, true);
    }

    @Transactional(readOnly = true)
    public PurchaseOrder requireOrder(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de compra", id));
    }

    private void requireActiveSupplier(UUID supplierId) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor", supplierId));
        if (!supplier.isActive()) {
            throw new BusinessRuleException("SUPPLIER_INACTIVE", "El proveedor se encuentra inactivo");
        }
    }

    private void rebuildItems(PurchaseOrder order, CreatePurchaseOrderRequest request) {
        Set<UUID> seen = new HashSet<>();
        order.getItems().clear();
        for (CreatePurchaseOrderRequest.ItemRequest itemReq : request.getItems()) {
            if (!seen.add(itemReq.getVariantId())) {
                throw new BusinessRuleException("DUPLICATE_ORDER_ITEM",
                        "La variante " + itemReq.getVariantId() + " está repetida en la orden");
            }
            ProductVariant variant = variantRepository.findById(itemReq.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Variante de producto", itemReq.getVariantId()));

            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setOrder(order);
            item.setVariantId(variant.getId());
            item.setQuantity(itemReq.getQuantity());
            item.setUnitCost(itemReq.getUnitCost());
            order.getItems().add(item);
        }
    }

    private void applyTotals(PurchaseOrder order) {
        BigDecimal subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (PurchaseOrderItem item : order.getItems()) {
            BigDecimal lineTotal = item.getQuantity().multiply(item.getUnitCost()).setScale(2, RoundingMode.HALF_UP);
            item.setLineTotal(lineTotal);
            subtotal = subtotal.add(lineTotal);
        }
        TaxRule taxRule = taxService.resolveDefault();
        BigDecimal taxTotal = taxService.taxAmount(subtotal, taxRule.getRate());
        order.setSubtotal(subtotal);
        order.setTaxTotal(taxTotal);
        order.setTotal(subtotal.add(taxTotal));
    }

    private void validateStatusValue(String status) {
        Set<String> allowed = Set.of("DRAFT", "ORDERED", "PARTIALLY_RECEIVED", "RECEIVED", "CANCELLED");
        if (!allowed.contains(status)) {
            throw new BusinessRuleException("INVALID_ORDER_STATUS",
                    "El estado debe ser DRAFT, ORDERED, PARTIALLY_RECEIVED, RECEIVED o CANCELLED");
        }
    }

    private String nextNumber(String prefix) {
        String datePrefix = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return prefix + datePrefix + "-" + randomSuffix;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private PurchaseOrderResponseDto map(PurchaseOrder order, Map<UUID, BigDecimal> received, boolean includeDetail) {
        PurchaseOrderResponseDto dto = new PurchaseOrderResponseDto();
        dto.setId(order.getId());
        dto.setSupplierId(order.getSupplierId());
        dto.setSupplierName(supplierRepository.findById(order.getSupplierId())
                .map(Supplier::getName)
                .orElse(null));
        dto.setOrderNumber(order.getOrderNumber());
        dto.setStatus(order.getStatus());
        dto.setOrderedAt(order.getOrderedAt());
        dto.setSubtotal(order.getSubtotal());
        dto.setTaxTotal(order.getTaxTotal());
        dto.setTotal(order.getTotal());
        dto.setNotes(order.getNotes());

        for (PurchaseOrderItem item : order.getItems()) {
            PurchaseOrderResponseDto.ItemDto itemDto = new PurchaseOrderResponseDto.ItemDto();
            itemDto.setId(item.getId());
            itemDto.setVariantId(item.getVariantId());
            itemDto.setQuantity(item.getQuantity());
            itemDto.setUnitCost(item.getUnitCost());
            itemDto.setLineTotal(item.getLineTotal());
            if (includeDetail) {
                itemDto.setSku(variantRepository.findById(item.getVariantId())
                        .map(ProductVariant::getSku)
                        .orElse(null));
                itemDto.setReceivedQuantity(received != null
                        ? received.getOrDefault(item.getId(), BigDecimal.ZERO)
                        : BigDecimal.ZERO);
            }
            dto.getItems().add(itemDto);
        }
        return dto;
    }
}
