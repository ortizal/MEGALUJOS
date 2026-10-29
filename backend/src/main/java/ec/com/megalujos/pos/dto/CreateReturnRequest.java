package ec.com.megalujos.pos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class CreateReturnRequest {

    @NotBlank(message = "El motivo de la devolución es obligatorio")
    private String reason;

    @NotEmpty(message = "La devolución debe incluir al menos un ítem")
    @Valid
    private List<ReturnItemRequest> items;

    /** CASH (por defecto), TRANSFER, CARD u OTHER. */
    private String paymentMethod;

    public CreateReturnRequest() {
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public List<ReturnItemRequest> getItems() {
        return items;
    }

    public void setItems(List<ReturnItemRequest> items) {
        this.items = items;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public static class ReturnItemRequest {

        @NotNull(message = "El ítem de la venta es obligatorio")
        private UUID saleItemId;

        @NotNull(message = "La cantidad a devolver es obligatoria")
        @Positive(message = "La cantidad a devolver debe ser mayor a cero")
        private BigDecimal quantity;

        /** Reintegrar al inventario (por defecto sí). */
        private Boolean restock = Boolean.TRUE;

        public ReturnItemRequest() {
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

        public Boolean getRestock() {
            return restock;
        }

        public void setRestock(Boolean restock) {
            this.restock = restock;
        }
    }
}
