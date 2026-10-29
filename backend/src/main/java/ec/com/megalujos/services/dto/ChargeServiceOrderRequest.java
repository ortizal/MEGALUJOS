package ec.com.megalujos.services.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChargeServiceOrderRequest {

    @NotNull(message = "La sesión de caja es obligatoria")
    private UUID cashSessionId;

    /** Pagos recibidos por el saldo restante (los anticipos se aplican automáticamente). */
    @Valid
    private List<PaymentItem> payments = new ArrayList<>();

    public ChargeServiceOrderRequest() {
    }

    public UUID getCashSessionId() {
        return cashSessionId;
    }

    public void setCashSessionId(UUID cashSessionId) {
        this.cashSessionId = cashSessionId;
    }

    public List<PaymentItem> getPayments() {
        return payments;
    }

    public void setPayments(List<PaymentItem> payments) {
        this.payments = payments != null ? payments : new ArrayList<>();
    }

    public static class PaymentItem {

        @NotNull(message = "El medio de pago es obligatorio")
        private String method;

        @NotNull(message = "El monto es obligatorio")
        @Positive(message = "El monto debe ser mayor a cero")
        private BigDecimal amount;

        private String reference;

        public PaymentItem() {
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getReference() {
            return reference;
        }

        public void setReference(String reference) {
            this.reference = reference;
        }
    }
}
