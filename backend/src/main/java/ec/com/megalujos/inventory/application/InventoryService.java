package ec.com.megalujos.inventory.application;

import ec.com.megalujos.catalog.domain.ProductVariant;
import ec.com.megalujos.catalog.domain.ProductVariantRepository;
import ec.com.megalujos.common.audit.AuditService;
import ec.com.megalujos.common.exception.BusinessRuleException;
import ec.com.megalujos.common.exception.ResourceNotFoundException;
import ec.com.megalujos.inventory.domain.*;
import ec.com.megalujos.inventory.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class InventoryService {

    private final StockBalanceRepository stockBalanceRepository;
    private final StockMovementRepository stockMovementRepository;
    private final InventoryAdjustmentRepository adjustmentRepository;
    private final StockCountRepository stockCountRepository;
    private final ProductVariantRepository variantRepository;
    private final StockService stockService;
    private final AuditService auditService;

    public InventoryService(StockBalanceRepository stockBalanceRepository,
                            StockMovementRepository stockMovementRepository,
                            InventoryAdjustmentRepository adjustmentRepository,
                            StockCountRepository stockCountRepository,
                            ProductVariantRepository variantRepository,
                            StockService stockService,
                            AuditService auditService) {
        this.stockBalanceRepository = stockBalanceRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.adjustmentRepository = adjustmentRepository;
        this.stockCountRepository = stockCountRepository;
        this.variantRepository = variantRepository;
        this.stockService = stockService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<StockBalanceDto> listBalances(String q, UUID warehouseId, boolean lowStockOnly, Pageable pageable) {
        Page<StockBalanceRow> rows = stockBalanceRepository.searchBalances(q, warehouseId, lowStockOnly, pageable);
        return rows.map(this::mapBalanceRow);
    }

    @Transactional(readOnly = true)
    public Page<StockMovementDto> listMovements(UUID variantId, UUID warehouseId, String movementType,
                                                OffsetDateTime dateFrom, OffsetDateTime dateTo, Pageable pageable) {
        Page<StockMovement> movements = stockMovementRepository.searchKardex(
                variantId, warehouseId, movementType, dateFrom, dateTo, pageable);
        return movements.map(this::mapMovement);
    }

    @Transactional(readOnly = true)
    public List<StockAlertDto> listAlerts() {
        List<StockAlertDto> alerts = new ArrayList<>();
        for (StockAlertRow row : stockBalanceRepository.findLowStockAlerts()) {
            StockAlertDto dto = new StockAlertDto();
            dto.setVariantId(row.getVariantId());
            dto.setSku(row.getSku());
            dto.setProductName(row.getProductName());
            dto.setQuantity(row.getQuantity());
            dto.setMinStock(row.getMinStock());
            alerts.add(dto);
        }
        return alerts;
    }

    @Transactional
    public AdjustmentResponseDto createAdjustment(CreateAdjustmentRequest request, UUID actorId) {
        UUID warehouseId = request.getWarehouseId() != null ? request.getWarehouseId() : stockService.getMainWarehouseId();

        InventoryAdjustment adjustment = new InventoryAdjustment();
        adjustment.setAdjustmentNumber(nextNumber("AJE-"));
        adjustment.setWarehouseId(warehouseId);
        adjustment.setStatus("DRAFT");
        adjustment.setReason(request.getReason());
        adjustment.setCreatedBy(actorId);

        for (CreateAdjustmentRequest.ItemRequest itemReq : request.getItems()) {
            ProductVariant variant = variantRepository.findById(itemReq.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Variante de producto", itemReq.getVariantId()));

            BigDecimal current = stockService.getAvailableStock(variant.getId(), warehouseId);
            InventoryAdjustmentItem item = new InventoryAdjustmentItem();
            item.setAdjustment(adjustment);
            item.setVariantId(variant.getId());
            item.setCurrentQuantity(current);
            item.setAdjustedQuantity(itemReq.getAdjustedQuantity());
            item.setDifference(itemReq.getAdjustedQuantity().subtract(current));
            item.setCost(itemReq.getCost() != null ? itemReq.getCost() : variant.getCostPrice());
            adjustment.getItems().add(item);
        }

        InventoryAdjustment saved = adjustmentRepository.save(adjustment);
        return mapAdjustment(saved);
    }

    @Transactional
    public AdjustmentResponseDto approveAdjustment(UUID id, UUID actorId) {
        InventoryAdjustment adjustment = adjustmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ajuste de inventario", id));

        if (!"DRAFT".equalsIgnoreCase(adjustment.getStatus())) {
            throw new BusinessRuleException("ADJUSTMENT_NOT_DRAFT", "Solo los ajustes en estado DRAFT pueden aprobarse");
        }

        for (InventoryAdjustmentItem item : adjustment.getItems()) {
            if (item.getDifference().compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            if (item.getDifference().compareTo(BigDecimal.ZERO) > 0) {
                stockService.addStock(
                        item.getVariantId(),
                        adjustment.getWarehouseId(),
                        item.getDifference(),
                        item.getCost(),
                        "ADJUSTMENT_IN",
                        "INVENTORY_ADJUSTMENT",
                        adjustment.getId(),
                        item.getId(),
                        actorId,
                        "Ajuste " + adjustment.getAdjustmentNumber()
                );
            } else {
                stockService.deductStock(
                        item.getVariantId(),
                        adjustment.getWarehouseId(),
                        item.getDifference().negate(),
                        "ADJUSTMENT_OUT",
                        "INVENTORY_ADJUSTMENT",
                        adjustment.getId(),
                        item.getId(),
                        actorId,
                        "Ajuste " + adjustment.getAdjustmentNumber()
                );
            }
        }

        adjustment.setStatus("APPROVED");
        adjustment.setApprovedBy(actorId);
        adjustment.setApprovedAt(OffsetDateTime.now());
        InventoryAdjustment saved = adjustmentRepository.save(adjustment);

        auditService.record(actorId, "ADJUSTMENT_APPROVED", "InventoryAdjustment", saved.getId(),
                Map.of("adjustmentNumber", saved.getAdjustmentNumber(), "warehouseId", saved.getWarehouseId().toString()),
                null);

        return mapAdjustment(saved);
    }

    @Transactional(readOnly = true)
    public Page<AdjustmentResponseDto> listAdjustments(Pageable pageable) {
        return adjustmentRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::mapAdjustment);
    }

    @Transactional(readOnly = true)
    public AdjustmentResponseDto getAdjustment(UUID id) {
        InventoryAdjustment adjustment = adjustmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ajuste de inventario", id));
        return mapAdjustment(adjustment);
    }

    @Transactional(readOnly = true)
    public Page<CountResponseDto> listCounts(Pageable pageable) {
        return stockCountRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::mapCount);
    }

    @Transactional
    public CountResponseDto createCount(CreateCountRequest request, UUID actorId) {
        UUID warehouseId = request.getWarehouseId() != null ? request.getWarehouseId() : stockService.getMainWarehouseId();

        StockCount count = new StockCount();
        count.setCountNumber(nextNumber("CNT-"));
        count.setWarehouseId(warehouseId);
        count.setStatus("OPEN");
        count.setNotes(request.getNotes());
        count.setCreatedBy(actorId);

        Set<UUID> seen = new HashSet<>();
        for (CreateCountRequest.ItemRequest itemReq : request.getItems()) {
            if (!seen.add(itemReq.getVariantId())) {
                throw new BusinessRuleException("DUPLICATE_COUNT_ITEM",
                        "La variante " + itemReq.getVariantId() + " está repetida en el conteo");
            }
            variantRepository.findById(itemReq.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Variante de producto", itemReq.getVariantId()));

            BigDecimal system = stockService.getAvailableStock(itemReq.getVariantId(), warehouseId);
            StockCountItem item = new StockCountItem();
            item.setCount(count);
            item.setVariantId(itemReq.getVariantId());
            item.setSystemQuantity(system);
            item.setCountedQuantity(itemReq.getCountedQuantity());
            item.setDifference(itemReq.getCountedQuantity().subtract(system));
            count.getItems().add(item);
        }

        StockCount saved = stockCountRepository.save(count);
        return mapCount(saved);
    }

    @Transactional
    public CountResponseDto completeCount(UUID id) {
        StockCount count = stockCountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conteo de inventario", id));

        if (!"OPEN".equalsIgnoreCase(count.getStatus())) {
            throw new BusinessRuleException("COUNT_NOT_OPEN", "Solo los conteos en estado OPEN pueden completarse");
        }

        count.setStatus("COUNTED");
        count.setCompletedAt(OffsetDateTime.now());
        return mapCount(stockCountRepository.save(count));
    }

    @Transactional
    public CountResponseDto approveCount(UUID id, UUID actorId) {
        StockCount count = stockCountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conteo de inventario", id));

        if (!"COUNTED".equalsIgnoreCase(count.getStatus())) {
            throw new BusinessRuleException("COUNT_NOT_COUNTED", "Solo los conteos en estado COUNTED pueden aprobarse");
        }

        for (StockCountItem item : count.getItems()) {
            if (item.getDifference().compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            if (item.getDifference().compareTo(BigDecimal.ZERO) > 0) {
                stockService.addStock(
                        item.getVariantId(),
                        count.getWarehouseId(),
                        item.getDifference(),
                        null,
                        "ADJUSTMENT_IN",
                        "STOCK_COUNT",
                        count.getId(),
                        item.getId(),
                        actorId,
                        "Conteo " + count.getCountNumber()
                );
            } else {
                stockService.deductStock(
                        item.getVariantId(),
                        count.getWarehouseId(),
                        item.getDifference().negate(),
                        "ADJUSTMENT_OUT",
                        "STOCK_COUNT",
                        count.getId(),
                        item.getId(),
                        actorId,
                        "Conteo " + count.getCountNumber()
                );
            }
        }

        count.setStatus("APPROVED");
        count.setApprovedBy(actorId);
        count.setApprovedAt(OffsetDateTime.now());
        StockCount saved = stockCountRepository.save(count);

        auditService.record(actorId, "STOCK_COUNT_APPROVED", "StockCount", saved.getId(),
                Map.of("countNumber", saved.getCountNumber(), "warehouseId", saved.getWarehouseId().toString()),
                null);

        return mapCount(saved);
    }

    @Transactional(readOnly = true)
    public CountResponseDto getCount(UUID id) {
        StockCount count = stockCountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conteo de inventario", id));
        return mapCount(count);
    }

    private String nextNumber(String prefix) {
        String datePrefix = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return prefix + datePrefix + "-" + randomSuffix;
    }

    private StockBalanceDto mapBalanceRow(StockBalanceRow row) {
        StockBalanceDto dto = new StockBalanceDto();
        dto.setVariantId(row.getVariantId());
        dto.setWarehouseId(row.getWarehouseId());
        dto.setSku(row.getSku());
        dto.setProductName(row.getProductName());
        dto.setWarehouseName(row.getWarehouseName());
        dto.setQuantity(row.getQuantity());
        dto.setReserved(row.getReserved());
        BigDecimal available = row.getQuantity().subtract(row.getReserved() != null ? row.getReserved() : BigDecimal.ZERO);
        dto.setAvailable(available.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : available);
        dto.setMinStock(row.getMinStock());
        return dto;
    }

    private StockMovementDto mapMovement(StockMovement movement) {
        StockMovementDto dto = new StockMovementDto();
        dto.setId(movement.getId());
        dto.setVariantId(movement.getVariantId());
        dto.setWarehouseId(movement.getWarehouseId());
        dto.setMovementType(movement.getMovementType());
        dto.setQuantityDelta(movement.getQuantityDelta());
        dto.setBalanceAfter(movement.getBalanceAfter());
        dto.setUnitCost(movement.getUnitCost());
        dto.setSourceType(movement.getSourceType());
        dto.setSourceId(movement.getSourceId());
        dto.setNotes(movement.getNotes());
        dto.setCreatedAt(movement.getCreatedAt());
        return dto;
    }

    private AdjustmentResponseDto mapAdjustment(InventoryAdjustment adjustment) {
        AdjustmentResponseDto dto = new AdjustmentResponseDto();
        dto.setId(adjustment.getId());
        dto.setAdjustmentNumber(adjustment.getAdjustmentNumber());
        dto.setWarehouseId(adjustment.getWarehouseId());
        dto.setStatus(adjustment.getStatus());
        dto.setReason(adjustment.getReason());
        dto.setCreatedBy(adjustment.getCreatedBy());
        dto.setApprovedBy(adjustment.getApprovedBy());
        dto.setCreatedAt(adjustment.getCreatedAt());
        dto.setApprovedAt(adjustment.getApprovedAt());

        List<AdjustmentResponseDto.ItemResponseDto> items = new ArrayList<>();
        for (InventoryAdjustmentItem item : adjustment.getItems()) {
            AdjustmentResponseDto.ItemResponseDto idto = new AdjustmentResponseDto.ItemResponseDto();
            idto.setId(item.getId());
            idto.setVariantId(item.getVariantId());
            idto.setCurrentQuantity(item.getCurrentQuantity());
            idto.setAdjustedQuantity(item.getAdjustedQuantity());
            idto.setDifference(item.getDifference());
            idto.setCost(item.getCost());
            items.add(idto);
        }
        dto.setItems(items);
        return dto;
    }

    private CountResponseDto mapCount(StockCount count) {
        CountResponseDto dto = new CountResponseDto();
        dto.setId(count.getId());
        dto.setCountNumber(count.getCountNumber());
        dto.setWarehouseId(count.getWarehouseId());
        dto.setStatus(count.getStatus());
        dto.setNotes(count.getNotes());
        dto.setCreatedBy(count.getCreatedBy());
        dto.setApprovedBy(count.getApprovedBy());
        dto.setCreatedAt(count.getCreatedAt());
        dto.setCompletedAt(count.getCompletedAt());
        dto.setApprovedAt(count.getApprovedAt());

        List<CountResponseDto.ItemResponseDto> items = new ArrayList<>();
        for (StockCountItem item : count.getItems()) {
            CountResponseDto.ItemResponseDto idto = new CountResponseDto.ItemResponseDto();
            idto.setId(item.getId());
            idto.setVariantId(item.getVariantId());
            idto.setSystemQuantity(item.getSystemQuantity());
            idto.setCountedQuantity(item.getCountedQuantity());
            idto.setDifference(item.getDifference());
            items.add(idto);
        }
        dto.setItems(items);
        return dto;
    }
}
