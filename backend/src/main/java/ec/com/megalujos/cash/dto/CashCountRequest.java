package ec.com.megalujos.cash.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CashCountRequest {

    @NotEmpty(message = "Debe registrar al menos un medio de pago contado")
    @Valid
    private List<CountItem> counts = new ArrayList<>();

    public CashCountRequest() {
    }

    public List<CountItem> getCounts() {
        return counts;
    }

    public void setCounts(List<CountItem> counts) {
        this.counts = counts;
    }

    public static class CountItem {

        @NotBlank(message = "El medio de pago es obligatorio")
        private String method;

        @NotNull(message = "El monto contado es obligatorio")
        @DecimalMin(value = "0", message = "El monto contado no puede ser negativo")
        private BigDecimal countedAmount;

        public CountItem() {
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public BigDecimal getCountedAmount() {
            return countedAmount;
        }

        public void setCountedAmount(BigDecimal countedAmount) {
            this.countedAmount = countedAmount;
        }
    }
}
