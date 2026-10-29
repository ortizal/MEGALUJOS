package ec.com.megalujos.pos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class ConfirmSaleRequest {

    @NotNull(message = "La sesión de caja es obligatoria")
    private UUID cashSessionId;

    private UUID customerId;

    @NotEmpty(message = "La venta debe contener al menos un producto o servicio")
    @Valid
    private List<SaleItemRequest> items;

    @NotEmpty(message = "Debe registrar al menos un medio de pago")
    @Valid
    private List<SalePaymentRequest> payments;

    private boolean requestInvoice = false;

    private String notes;

    public ConfirmSaleRequest() {
    }

    public UUID getCashSessionId() {
        return cashSessionId;
    }

    public void setCashSessionId(UUID cashSessionId) {
        this.cashSessionId = cashSessionId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public List<SaleItemRequest> getItems() {
        return items;
    }

    public void setItems(List<SaleItemRequest> items) {
        this.items = items;
    }

    public List<SalePaymentRequest> getPayments() {
        return payments;
    }

    public void setPayments(List<SalePaymentRequest> payments) {
        this.payments = payments;
    }

    public boolean isRequestInvoice() {
        return requestInvoice;
    }

    public void setRequestInvoice(boolean requestInvoice) {
        this.requestInvoice = requestInvoice;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public static class SaleItemRequest {
        @NotNull(message = "El variantId es obligatorio")
        private UUID variantId;

        @NotNull(message = "La cantidad es obligatoria")
        private BigDecimal quantity;

        private BigDecimal discount = BigDecimal.ZERO;

        public SaleItemRequest() {
        }

        public UUID getVariantId() {
            return variantId;
        }

        public void setVariantId(UUID variantId) {
            this.variantId = variantId;
        }

        public BigDecimal getQuantity() {
            return quantity;
        }

        public void setQuantity(BigDecimal quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getDiscount() {
            return discount;
        }

        public void setDiscount(BigDecimal discount) {
            this.discount = discount;
        }
    }

    public static class SalePaymentRequest {
        @NotNull(message = "El método de pago es obligatorio")
        private String method; // CASH, TRANSFER, CARD, OTHER, APPLIED_ADVANCE

        @NotNull(message = "El monto es obligatorio")
        private BigDecimal amount;

        private String reference;

        public SalePaymentRequest() {
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
