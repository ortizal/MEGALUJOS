package ec.com.megalujos.services.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CreateServiceOrderRequest {

    @NotNull(message = "El cliente es obligatorio")
    private UUID customerId;

    private UUID vehicleId;

    @NotBlank(message = "El tipo de servicio es obligatorio")
    private String serviceType; // STICKER, INSTALLATION, OTHER

    private String description;

    private String dimensions;

    private OffsetDateTime scheduledAt;

    @NotNull(message = "La mano de obra es obligatoria")
    @DecimalMin(value = "0", message = "La mano de obra no puede ser negativa")
    private BigDecimal laborAmount = BigDecimal.ZERO;

    private UUID assignedUserId;

    /** Si es true la orden nace CONFIRMED; por defecto nace QUOTED (cotización). */
    private boolean confirmed = false;

    @Valid
    private List<ItemRequest> items = new ArrayList<>();

    /** Materiales previstos: solo planned_quantity, no descuenta stock. */
    @Valid
    private List<MaterialRequest> materials = new ArrayList<>();

    private List<UUID> attachmentIds = new ArrayList<>();

    public CreateServiceOrderRequest() {
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public UUID getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(UUID vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDimensions() {
        return dimensions;
    }

    public void setDimensions(String dimensions) {
        this.dimensions = dimensions;
    }

    public OffsetDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(OffsetDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public BigDecimal getLaborAmount() {
        return laborAmount;
    }

    public void setLaborAmount(BigDecimal laborAmount) {
        this.laborAmount = laborAmount;
    }

    public UUID getAssignedUserId() {
        return assignedUserId;
    }

    public void setAssignedUserId(UUID assignedUserId) {
        this.assignedUserId = assignedUserId;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    public List<ItemRequest> getItems() {
        return items;
    }

    public void setItems(List<ItemRequest> items) {
        this.items = items;
    }

    public List<MaterialRequest> getMaterials() {
        return materials;
    }

    public void setMaterials(List<MaterialRequest> materials) {
        this.materials = materials;
    }

    public List<UUID> getAttachmentIds() {
        return attachmentIds;
    }

    public void setAttachmentIds(List<UUID> attachmentIds) {
        this.attachmentIds = attachmentIds;
    }

    public static class ItemRequest {

        @NotBlank(message = "La descripción del ítem es obligatoria")
        private String description;

        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor a cero")
        private BigDecimal quantity = BigDecimal.ONE;

        @NotNull(message = "El precio unitario es obligatorio")
        @DecimalMin(value = "0", message = "El precio unitario no puede ser negativo")
        private BigDecimal unitPrice = BigDecimal.ZERO;

        public ItemRequest() {
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public BigDecimal getQuantity() {
            return quantity;
        }

        public void setQuantity(BigDecimal quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public void setUnitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
        }
    }

    public static class MaterialRequest {

        @NotNull(message = "La variante del material es obligatoria")
        private UUID variantId;

        @NotNull(message = "La cantidad planificada es obligatoria")
        @DecimalMin(value = "0", message = "La cantidad planificada no puede ser negativa")
        private BigDecimal plannedQuantity = BigDecimal.ZERO;

        public MaterialRequest() {
        }

        public UUID getVariantId() {
            return variantId;
        }

        public void setVariantId(UUID variantId) {
            this.variantId = variantId;
        }

        public BigDecimal getPlannedQuantity() {
            return plannedQuantity;
        }

        public void setPlannedQuantity(BigDecimal plannedQuantity) {
            this.plannedQuantity = plannedQuantity;
        }
    }
}
