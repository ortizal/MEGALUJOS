package ec.com.megalujos.services.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public class CreateAdvanceRequest {

    @NotNull(message = "El monto del anticipo es obligatorio")
    @Positive(message = "El monto del anticipo debe ser mayor a cero")
    private BigDecimal amount;

    @NotNull(message = "La sesión de caja es obligatoria")
    private UUID cashSessionId;

    @NotBlank(message = "El medio de pago es obligatorio")
    private String paymentMethod; // CASH, TRANSFER, CARD, OTHER

    private String reference;

    public CreateAdvanceRequest() {
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public UUID getCashSessionId() {
        return cashSessionId;
    }

    public void setCashSessionId(UUID cashSessionId) {
        this.cashSessionId = cashSessionId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }
}
