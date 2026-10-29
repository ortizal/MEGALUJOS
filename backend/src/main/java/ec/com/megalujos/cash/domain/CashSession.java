package ec.com.megalujos.cash.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "cash_sessions")
public class CashSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "register_id", nullable = false)
    private CashRegister register;

    @Column(name = "opened_by", nullable = false)
    private UUID openedBy;

    @Column(name = "closed_by")
    private UUID closedBy;

    @Column(name = "opened_at", nullable = false, updatable = false)
    private OffsetDateTime openedAt = OffsetDateTime.now();

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @Column(name = "opening_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal openingAmount = BigDecimal.ZERO;

    @Column(name = "counted_amount", precision = 18, scale = 2)
    private BigDecimal countedAmount;

    @Column(name = "expected_amount", precision = 18, scale = 2)
    private BigDecimal expectedAmount;

    @Column(name = "difference_amount", precision = 18, scale = 2)
    private BigDecimal differenceAmount;

    @Column(nullable = false, length = 20)
    private String status = "OPEN"; // OPEN, CLOSED

    @Column(columnDefinition = "TEXT")
    private String notes;

    public CashSession() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public CashRegister getRegister() {
        return register;
    }

    public void setRegister(CashRegister register) {
        this.register = register;
    }

    public UUID getOpenedBy() {
        return openedBy;
    }

    public void setOpenedBy(UUID openedBy) {
        this.openedBy = openedBy;
    }

    public UUID getClosedBy() {
        return closedBy;
    }

    public void setClosedBy(UUID closedBy) {
        this.closedBy = closedBy;
    }

    public OffsetDateTime getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(OffsetDateTime openedAt) {
        this.openedAt = openedAt;
    }

    public OffsetDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(OffsetDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public BigDecimal getOpeningAmount() {
        return openingAmount;
    }

    public void setOpeningAmount(BigDecimal openingAmount) {
        this.openingAmount = openingAmount;
    }

    public BigDecimal getCountedAmount() {
        return countedAmount;
    }

    public void setCountedAmount(BigDecimal countedAmount) {
        this.countedAmount = countedAmount;
    }

    public BigDecimal getExpectedAmount() {
        return expectedAmount;
    }

    public void setExpectedAmount(BigDecimal expectedAmount) {
        this.expectedAmount = expectedAmount;
    }

    public BigDecimal getDifferenceAmount() {
        return differenceAmount;
    }

    public void setDifferenceAmount(BigDecimal differenceAmount) {
        this.differenceAmount = differenceAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
