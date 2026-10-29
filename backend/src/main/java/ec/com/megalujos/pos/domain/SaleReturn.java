package ec.com.megalujos.pos.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Devolución de una venta (parcial o total). Nunca modifica la venta original. */
@Entity
@Table(name = "sale_returns")
public class SaleReturn {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "return_number", nullable = false, unique = true, length = 40)
    private String returnNumber;

    @Column(name = "sale_id", nullable = false)
    private UUID saleId;

    @Column(name = "cash_session_id", nullable = false)
    private UUID cashSessionId;

    @Column(name = "total_returned", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalReturned = BigDecimal.ZERO;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "authorized_by", nullable = false)
    private UUID authorizedBy;

    @Column(name = "payment_method", nullable = false, length = 30)
    private String paymentMethod = "CASH"; // CASH, TRANSFER, CARD, OTHER

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @OneToMany(mappedBy = "saleReturn", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<SaleReturnItem> items = new ArrayList<>();

    public SaleReturn() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getReturnNumber() {
        return returnNumber;
    }

    public void setReturnNumber(String returnNumber) {
        this.returnNumber = returnNumber;
    }

    public UUID getSaleId() {
        return saleId;
    }

    public void setSaleId(UUID saleId) {
        this.saleId = saleId;
    }

    public UUID getCashSessionId() {
        return cashSessionId;
    }

    public void setCashSessionId(UUID cashSessionId) {
        this.cashSessionId = cashSessionId;
    }

    public BigDecimal getTotalReturned() {
        return totalReturned;
    }

    public void setTotalReturned(BigDecimal totalReturned) {
        this.totalReturned = totalReturned;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public UUID getAuthorizedBy() {
        return authorizedBy;
    }

    public void setAuthorizedBy(UUID authorizedBy) {
        this.authorizedBy = authorizedBy;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<SaleReturnItem> getItems() {
        return items;
    }

    public void setItems(List<SaleReturnItem> items) {
        this.items = items;
    }
}
