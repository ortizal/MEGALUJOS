package ec.com.megalujos.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class CreateAdjustmentRequest {

    private UUID warehouseId;

    @NotBlank(message = "El motivo del ajuste es obligatorio")
    private String reason;

    @NotEmpty(message = "El ajuste debe contener al menos un ítem")
    @Valid
    private List<ItemRequest> items;

    public CreateAdjustmentRequest() {
    }

    public UUID getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(UUID warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public List<ItemRequest> getItems() {
        return items;
    }

    public void setItems(List<ItemRequest> items) {
        this.items = items;
    }

    public static class ItemRequest {

        @NotNull(message = "El variantId es obligatorio")
        private UUID variantId;

        @NotNull(message = "La cantidad ajustada es obligatoria")
        @DecimalMin(value = "0", message = "La cantidad ajustada no puede ser negativa")
        private BigDecimal adjustedQuantity;

        @DecimalMin(value = "0", message = "El costo no puede ser negativo")
        private BigDecimal cost;

        public ItemRequest() {
        }

        public UUID getVariantId() {
            return variantId;
        }

        public void setVariantId(UUID variantId) {
            this.variantId = variantId;
        }

        public BigDecimal getAdjustedQuantity() {
            return adjustedQuantity;
        }

        public void setAdjustedQuantity(BigDecimal adjustedQuantity) {
            this.adjustedQuantity = adjustedQuantity;
        }

        public BigDecimal getCost() {
            return cost;
        }

        public void setCost(BigDecimal cost) {
            this.cost = cost;
        }
    }
}
