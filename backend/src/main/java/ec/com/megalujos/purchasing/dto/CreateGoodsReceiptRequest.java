package ec.com.megalujos.purchasing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class CreateGoodsReceiptRequest {

    private UUID warehouseId;

    private String notes;

    @NotEmpty(message = "La recepción debe contener al menos un ítem")
    @Valid
    private List<ItemRequest> items;

    public CreateGoodsReceiptRequest() {
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

        @NotNull(message = "El orderItemId es obligatorio")
        private UUID orderItemId;

        @NotNull(message = "La cantidad recibida es obligatoria")
        @Positive(message = "La cantidad recibida debe ser mayor a cero")
        private BigDecimal quantity;

        public ItemRequest() {
        }

        public UUID getOrderItemId() {
            return orderItemId;
        }

        public void setOrderItemId(UUID orderItemId) {
            this.orderItemId = orderItemId;
        }

        public BigDecimal getQuantity() {
            return quantity;
        }

        public void setQuantity(BigDecimal quantity) {
            this.quantity = quantity;
        }
    }
}
