package ec.com.megalujos.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class CreateCountRequest {

    private UUID warehouseId;

    private String notes;

    @NotEmpty(message = "El conteo debe contener al menos un ítem")
    @Valid
    private List<ItemRequest> items;

    public CreateCountRequest() {
    }

    public UUID getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(UUID warehouseId) {
        this.warehouseId = warehouseId;
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

        @NotNull(message = "La cantidad contada es obligatoria")
        @PositiveOrZero(message = "La cantidad contada no puede ser negativa")
        private BigDecimal countedQuantity;

        public ItemRequest() {
        }

        public UUID getVariantId() {
            return variantId;
        }

        public void setVariantId(UUID variantId) {
            this.variantId = variantId;
        }

        public BigDecimal getCountedQuantity() {
            return countedQuantity;
        }

        public void setCountedQuantity(BigDecimal countedQuantity) {
            this.countedQuantity = countedQuantity;
        }
    }
}
