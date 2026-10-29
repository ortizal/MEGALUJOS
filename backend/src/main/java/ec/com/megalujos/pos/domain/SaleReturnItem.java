package ec.com.megalujos.pos.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

/** Línea de devolución asociada a un ítem de la venta original. */
@Entity
@Table(name = "sale_return_items")
public class SaleReturnItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_return_id", nullable = false)
    @JsonIgnore
    private SaleReturn saleReturn;

    @Column(name = "sale_item_id", nullable = false)
    private UUID saleItemId;

    @Column(nullable = false, precision = 18, scale = 3)
    private BigDecimal quantity;

    @Column(name = "return_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal returnAmount;

    @Column(nullable = false)
    private boolean restock = true;

    public SaleReturnItem() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public SaleReturn getSaleReturn() {
        return saleReturn;
    }

    public void setSaleReturn(SaleReturn saleReturn) {
        this.saleReturn = saleReturn;
    }

    public UUID getSaleItemId() {
        return saleItemId;
    }

    public void setSaleItemId(UUID saleItemId) {
        this.saleItemId = saleItemId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getReturnAmount() {
        return returnAmount;
    }

    public void setReturnAmount(BigDecimal returnAmount) {
        this.returnAmount = returnAmount;
    }

    public boolean isRestock() {
        return restock;
    }

    public void setRestock(boolean restock) {
        this.restock = restock;
    }
}
