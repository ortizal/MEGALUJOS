package ec.com.megalujos.inventory.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "stock_count_items")
public class StockCountItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "count_id", nullable = false)
    @JsonIgnore
    private StockCount count;

    @Column(name = "variant_id", nullable = false)
    private UUID variantId;

    @Column(name = "system_quantity", nullable = false, precision = 18, scale = 3)
    private BigDecimal systemQuantity = BigDecimal.ZERO;

    @Column(name = "counted_quantity", nullable = false, precision = 18, scale = 3)
    private BigDecimal countedQuantity = BigDecimal.ZERO;

    @Column(nullable = false, precision = 18, scale = 3)
    private BigDecimal difference = BigDecimal.ZERO;

    public StockCountItem() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public StockCount getCount() {
        return count;
    }

    public void setCount(StockCount count) {
        this.count = count;
    }

    public UUID getVariantId() {
        return variantId;
    }

    public void setVariantId(UUID variantId) {
        this.variantId = variantId;
    }

    public BigDecimal getSystemQuantity() {
        return systemQuantity;
    }

    public void setSystemQuantity(BigDecimal systemQuantity) {
        this.systemQuantity = systemQuantity;
    }

    public BigDecimal getCountedQuantity() {
        return countedQuantity;
    }

    public void setCountedQuantity(BigDecimal countedQuantity) {
        this.countedQuantity = countedQuantity;
    }

    public BigDecimal getDifference() {
        return difference;
    }

    public void setDifference(BigDecimal difference) {
        this.difference = difference;
    }
}
