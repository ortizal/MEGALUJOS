package ec.com.megalujos.inventory.application;

import ec.com.megalujos.common.exception.BusinessRuleException;
import ec.com.megalujos.inventory.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class StockService {

    private final StockBalanceRepository stockBalanceRepository;
    private final StockMovementRepository stockMovementRepository;
    private final WarehouseRepository warehouseRepository;

    public StockService(StockBalanceRepository stockBalanceRepository,
                        StockMovementRepository stockMovementRepository,
                        WarehouseRepository warehouseRepository) {
        this.stockBalanceRepository = stockBalanceRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.warehouseRepository = warehouseRepository;
    }

    @Transactional
    public StockMovement deductStock(UUID variantId, UUID warehouseId, BigDecimal quantity, String movementType,
                                     String sourceType, UUID sourceId, UUID sourceLineId, UUID actorId, String notes) {
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("INVALID_QUANTITY", "La cantidad a descontar debe ser mayor a cero");
        }

        UUID targetWarehouseId = warehouseId != null ? warehouseId : getMainWarehouseId();
        StockBalanceId balanceId = new StockBalanceId(variantId, targetWarehouseId);

        // Bloqueo pesimista para prevenir concurrencia y stock negativo
        StockBalance balance = stockBalanceRepository.findByIdForUpdate(balanceId)
                .orElseThrow(() -> new BusinessRuleException("STOCK_NOT_FOUND", "No existe registro de inventario para la variante en la bodega"));

        BigDecimal available = balance.getAvailableQuantity();
        if (available.compareTo(quantity) < 0) {
            throw new BusinessRuleException("INSUFFICIENT_STOCK",
                    String.format("Stock insuficiente. Disponible: %s, Solicitado: %s", available, quantity));
        }

        BigDecimal newQty = balance.getQuantity().subtract(quantity);
        balance.setQuantity(newQty);
        balance.setUpdatedAt(OffsetDateTime.now());
        stockBalanceRepository.save(balance);

        StockMovement movement = new StockMovement();
        movement.setVariantId(variantId);
        movement.setWarehouseId(targetWarehouseId);
        movement.setMovementType(movementType);
        movement.setQuantityDelta(quantity.negate());
        movement.setBalanceAfter(newQty);
        movement.setSourceType(sourceType);
        movement.setSourceId(sourceId);
        movement.setSourceLineId(sourceLineId);
        movement.setActorId(actorId);
        movement.setNotes(notes);

        return stockMovementRepository.save(movement);
    }

    @Transactional
    public StockMovement addStock(UUID variantId, UUID warehouseId, BigDecimal quantity, BigDecimal unitCost,
                                  String movementType, String sourceType, UUID sourceId, UUID sourceLineId,
                                  UUID actorId, String notes) {
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("INVALID_QUANTITY", "La cantidad a ingresar debe ser mayor a cero");
        }

        UUID targetWarehouseId = warehouseId != null ? warehouseId : getMainWarehouseId();
        StockBalanceId balanceId = new StockBalanceId(variantId, targetWarehouseId);

        StockBalance balance = stockBalanceRepository.findByIdForUpdate(balanceId)
                .orElseGet(() -> new StockBalance(balanceId, BigDecimal.ZERO, BigDecimal.ZERO));

        BigDecimal newQty = balance.getQuantity().add(quantity);
        balance.setQuantity(newQty);
        balance.setUpdatedAt(OffsetDateTime.now());
        stockBalanceRepository.save(balance);

        StockMovement movement = new StockMovement();
        movement.setVariantId(variantId);
        movement.setWarehouseId(targetWarehouseId);
        movement.setMovementType(movementType);
        movement.setQuantityDelta(quantity);
        movement.setBalanceAfter(newQty);
        movement.setUnitCost(unitCost);
        movement.setSourceType(sourceType);
        movement.setSourceId(sourceId);
        movement.setSourceLineId(sourceLineId);
        movement.setActorId(actorId);
        movement.setNotes(notes);

        return stockMovementRepository.save(movement);
    }

    @Transactional(readOnly = true)
    public BigDecimal getAvailableStock(UUID variantId, UUID warehouseId) {
        UUID targetWarehouseId = warehouseId != null ? warehouseId : getMainWarehouseId();
        StockBalanceId balanceId = new StockBalanceId(variantId, targetWarehouseId);
        return stockBalanceRepository.findById(balanceId)
                .map(StockBalance::getAvailableQuantity)
                .orElse(BigDecimal.ZERO);
    }

    public UUID getMainWarehouseId() {
        return warehouseRepository.findFirstByIsMainTrueAndActiveTrue()
                .map(Warehouse::getId)
                .orElseThrow(() -> new BusinessRuleException("NO_MAIN_WAREHOUSE", "No hay bodega principal configurada"));
    }
}
