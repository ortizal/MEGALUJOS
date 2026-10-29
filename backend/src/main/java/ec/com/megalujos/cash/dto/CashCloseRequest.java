package ec.com.megalujos.cash.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class CashCloseRequest {

    @NotNull(message = "El monto contado en efectivo es obligatorio")
    private BigDecimal countedAmount;

    private String notes;

    /** Conteos por medio de pago que se persisten como arqueo (cash_counts). */
    private List<CashCountRequest.CountItem> counts;

    public CashCloseRequest() {
    }

    public BigDecimal getCountedAmount() {
        return countedAmount;
    }

    public void setCountedAmount(BigDecimal countedAmount) {
        this.countedAmount = countedAmount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<CashCountRequest.CountItem> getCounts() {
        return counts;
    }

    public void setCounts(List<CashCountRequest.CountItem> counts) {
        this.counts = counts;
    }
}
