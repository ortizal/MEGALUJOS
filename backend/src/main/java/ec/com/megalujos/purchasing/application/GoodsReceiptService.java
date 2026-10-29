package ec.com.megalujos.purchasing.application;

import ec.com.megalujos.common.audit.AuditService;
import ec.com.megalujos.common.exception.BusinessRuleException;
import ec.com.megalujos.common.exception.ResourceNotFoundException;
import ec.com.megalujos.inventory.application.StockService;
import ec.com.megalujos.inventory.domain.WarehouseRepository;
import ec.com.megalujos.purchasing.domain.*;
import ec.com.megalujos.purchasing.dto.CreateGoodsReceiptRequest;
import ec.com.megalujos.purchasing.dto.GoodsReceiptResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class GoodsReceiptService {

    private final PurchaseOrderService purchaseOrderService;
    private final PurchaseOrderRepository orderRepository;
    private final GoodsReceiptRepository receiptRepository;
    private final GoodsReceiptItemRepository receiptItemRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockService stockService;
    private final AuditService auditService;

    public GoodsReceiptService(PurchaseOrderService purchaseOrderService,
                               PurchaseOrderRepository orderRepository,
                               GoodsReceiptRepository receiptRepository,
                               GoodsReceiptItemRepository receiptItemRepository,
                               WarehouseRepository warehouseRepository,
                               StockService stockService,
                               AuditService auditService) {
        this.purchaseOrderService = purchaseOrderService;
        this.orderRepository = orderRepository;
        this.receiptRepository = receiptRepository;
        this.receiptItemRepository = receiptItemRepository;
        this.warehouseRepository = warehouseRepository;
        this.stockService = stockService;
        this.auditService = auditService;
    }

    @Transactional
    public GoodsReceiptResponseDto receive(UUID orderId, CreateGoodsReceiptRequest request, UUID actorId) {
        PurchaseOrder order = purchaseOrderService.requireOrder(orderId);
        if (!"ORDERED".equals(order.getStatus()) && !"PARTIALLY_RECEIVED".equals(order.getStatus())) {
            throw new BusinessRuleException("ORDER_NOT_RECEIVABLE",
                    "Solo las órdenes en estado ORDERED o PARTIALLY_RECEIVED pueden recibir mercadería");
        }

        UUID warehouseId = request.getWarehouseId() != null
                ? request.getWarehouseId()
                : stockService.getMainWarehouseId();
        if (request.getWarehouseId() != null && !warehouseRepository.existsById(warehouseId)) {
            throw new ResourceNotFoundException("Bodega", warehouseId);
        }

        Map<UUID, PurchaseOrderItem> orderItems = new HashMap<>();
        for (PurchaseOrderItem item : order.getItems()) {
            orderItems.put(item.getId(), item);
        }

        Map<UUID, BigDecimal> pending = pendingByOrderItem(orderId, order);

        Map<UUID, BigDecimal> requested = new HashMap<>();
        for (CreateGoodsReceiptRequest.ItemRequest itemReq : request.getItems()) {
            PurchaseOrderItem orderItem = orderItems.get(itemReq.getOrderItemId());
            if (orderItem == null) {
                throw new BusinessRuleException("ORDER_ITEM_NOT_FOUND",
                        "La línea " + itemReq.getOrderItemId() + " no pertenece a la orden " + order.getOrderNumber());
            }
            BigDecimal pendingQty = pending.get(itemReq.getOrderItemId())
                    .subtract(requested.getOrDefault(itemReq.getOrderItemId(), BigDecimal.ZERO));
            if (itemReq.getQuantity().compareTo(pendingQty) > 0) {
                throw new BusinessRuleException("EXCESS_RECEIPT",
                        String.format("La cantidad recibida %s excede el pendiente %s de la línea %s",
                                itemReq.getQuantity(), pendingQty, itemReq.getOrderItemId()));
            }
            requested.merge(itemReq.getOrderItemId(), itemReq.getQuantity(), BigDecimal::add);
        }

        GoodsReceipt receipt = new GoodsReceipt();
        receipt.setOrderId(orderId);
        receipt.setWarehouseId(warehouseId);
        receipt.setReceiptNumber(nextNumber("REC-"));
        receipt.setStatus("RECEIVED");
        receipt.setNotes(normalize(request.getNotes()));
        GoodsReceipt saved = receiptRepository.save(receipt);

        for (CreateGoodsReceiptRequest.ItemRequest itemReq : request.getItems()) {
            PurchaseOrderItem orderItem = orderItems.get(itemReq.getOrderItemId());

            GoodsReceiptItem item = new GoodsReceiptItem();
            item.setReceipt(saved);
            item.setOrderItemId(orderItem.getId());
            item.setQuantity(itemReq.getQuantity());
            GoodsReceiptItem savedItem = receiptItemRepository.save(item);
            saved.getItems().add(savedItem);

            stockService.addStock(
                    orderItem.getVariantId(),
                    warehouseId,
                    itemReq.getQuantity(),
                    orderItem.getUnitCost(),
                    "PURCHASE",
                    "GOODS_RECEIPT",
                    saved.getId(),
                    savedItem.getId(),
                    actorId,
                    "Recepción " + saved.getReceiptNumber()
            );
        }

        boolean fullyReceived = true;
        Map<UUID, BigDecimal> receivedTotals = new HashMap<>();
        for (ReceivedQuantityRow row : receiptItemRepository.sumReceivedByOrder(orderId)) {
            receivedTotals.put(row.getOrderItemId(), row.getQuantity());
        }
        for (PurchaseOrderItem item : order.getItems()) {
            BigDecimal receivedQty = receivedTotals.getOrDefault(item.getId(), BigDecimal.ZERO);
            if (receivedQty.compareTo(item.getQuantity()) < 0) {
                fullyReceived = false;
                break;
            }
        }
        order.setStatus(fullyReceived ? "RECEIVED" : "PARTIALLY_RECEIVED");
        orderRepository.save(order);

        auditService.record(actorId, "GOODS_RECEIPT_CONFIRMED", "GoodsReceipt", saved.getId(),
                Map.of("receiptNumber", saved.getReceiptNumber(),
                        "orderNumber", order.getOrderNumber(),
                        "warehouseId", warehouseId.toString()),
                null);

        return map(saved);
    }

    @Transactional(readOnly = true)
    public Page<GoodsReceiptResponseDto> listReceipts(UUID orderId, Pageable pageable) {
        purchaseOrderService.requireOrder(orderId);
        return receiptRepository.findByOrderIdOrderByReceivedAtDesc(orderId, pageable).map(this::map);
    }

    private Map<UUID, BigDecimal> pendingByOrderItem(UUID orderId, PurchaseOrder order) {
        Map<UUID, BigDecimal> pending = new HashMap<>();
        for (PurchaseOrderItem item : order.getItems()) {
            pending.put(item.getId(), item.getQuantity());
        }
        for (ReceivedQuantityRow row : receiptItemRepository.sumReceivedByOrder(orderId)) {
            pending.computeIfPresent(row.getOrderItemId(),
                    (key, orderedQty) -> orderedQty.subtract(row.getQuantity()));
        }
        return pending;
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

    private GoodsReceiptResponseDto map(GoodsReceipt receipt) {
        GoodsReceiptResponseDto dto = new GoodsReceiptResponseDto();
        dto.setId(receipt.getId());
        dto.setOrderId(receipt.getOrderId());
        dto.setWarehouseId(receipt.getWarehouseId());
        dto.setReceiptNumber(receipt.getReceiptNumber());
        dto.setReceivedAt(receipt.getReceivedAt());
        dto.setStatus(receipt.getStatus());
        dto.setNotes(receipt.getNotes());

        for (GoodsReceiptItem item : receipt.getItems()) {
            GoodsReceiptResponseDto.ItemDto itemDto = new GoodsReceiptResponseDto.ItemDto();
            itemDto.setId(item.getId());
            itemDto.setOrderItemId(item.getOrderItemId());
            itemDto.setQuantity(item.getQuantity());
            dto.getItems().add(itemDto);
        }
        return dto;
    }
}
