package ec.com.megalujos.purchasing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class CreatePurchaseOrderRequest {

    @NotNull(message = "El proveedor es obligatorio")
    private UUID supplierId;

    private String notes;

    @NotEmpty(message = "La orden debe contener al menos un ítem")
    @Valid
    private List<ItemRequest> items;

    public CreatePurchaseOrderRequest() {
    }

    public UUID getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(UUID supplierId) {
        this.supplierId = supplierId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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

        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor a cero")
        private BigDecimal quantity;

        @NotNull(message = "El costo unitario es obligatorio")
        @DecimalMin(value = "0", message = "El costo unitario no puede ser negativo")
        private BigDecimal unitCost;

        public ItemRequest() {
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

        public BigDecimal getUnitCost() {
            return unitCost;
        }

        public void setUnitCost(BigDecimal unitCost) {
            this.unitCost = unitCost;
        }
    }
}
