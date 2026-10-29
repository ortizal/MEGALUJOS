package ec.com.megalujos.inventory.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "stock_balances")
public class StockBalance {

    @EmbeddedId
    private StockBalanceId id;

    @Column(nullable = false, precision = 18, scale = 3)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(nullable = false, precision = 18, scale = 3)
    private BigDecimal reserved = BigDecimal.ZERO;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public StockBalance() {
    }

    public StockBalance(StockBalanceId id, BigDecimal quantity, BigDecimal reserved) {
        this.id = id;
        this.quantity = quantity;
        this.reserved = reserved;
        this.updatedAt = OffsetDateTime.now();
    }

    public BigDecimal getAvailableQuantity() {
        BigDecimal avail = quantity.subtract(reserved != null ? reserved : BigDecimal.ZERO);
        return avail.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : avail;
    }

    public StockBalanceId getId() {
        return id;
    }

    public void setId(StockBalanceId id) {
        this.id = id;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getReserved() {
        return reserved;
    }

    public void setReserved(BigDecimal reserved) {
        this.reserved = reserved;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
