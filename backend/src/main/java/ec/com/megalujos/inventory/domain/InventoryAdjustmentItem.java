package ec.com.megalujos.inventory.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "inventory_adjustment_items")
public class InventoryAdjustmentItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adjustment_id", nullable = false)
    @JsonIgnore
    private InventoryAdjustment adjustment;

    @Column(name = "variant_id", nullable = false)
    private UUID variantId;

    @Column(name = "current_quantity", nullable = false, precision = 18, scale = 3)
    private BigDecimal currentQuantity = BigDecimal.ZERO;

    @Column(name = "adjusted_quantity", nullable = false, precision = 18, scale = 3)
    private BigDecimal adjustedQuantity = BigDecimal.ZERO;

    @Column(nullable = false, precision = 18, scale = 3)
    private BigDecimal difference = BigDecimal.ZERO;

    @Column(precision = 18, scale = 4)
    private BigDecimal cost;

    public InventoryAdjustmentItem() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public InventoryAdjustment getAdjustment() {
        return adjustment;
    }

    public void setAdjustment(InventoryAdjustment adjustment) {
        this.adjustment = adjustment;
    }

    public UUID getVariantId() {
        return variantId;
    }

    public void setVariantId(UUID variantId) {
        this.variantId = variantId;
    }

    public BigDecimal getCurrentQuantity() {
        return currentQuantity;
    }

    public void setCurrentQuantity(BigDecimal currentQuantity) {
        this.currentQuantity = currentQuantity;
    }

    public BigDecimal getAdjustedQuantity() {
        return adjustedQuantity;
    }

    public void setAdjustedQuantity(BigDecimal adjustedQuantity) {
        this.adjustedQuantity = adjustedQuantity;
    }

    public BigDecimal getDifference() {
        return difference;
    }

    public void setDifference(BigDecimal difference) {
        this.difference = difference;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }
}
