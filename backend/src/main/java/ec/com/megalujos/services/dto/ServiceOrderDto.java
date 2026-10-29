package ec.com.megalujos.services.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Detalle completo de una orden de servicio. */
public class ServiceOrderDto {

    private UUID id;
    private String orderNumber;
    private UUID customerId;
    private UUID vehicleId;
    private String serviceType;
    private String description;
    private String dimensions;
    private String status;
    private UUID assignedUserId;
    private BigDecimal laborAmount;
    private BigDecimal estimatedTotal;
    private BigDecimal advancesTotal = BigDecimal.ZERO;
    private BigDecimal remainingBalance = BigDecimal.ZERO;
    private OffsetDateTime scheduledAt;
    private OffsetDateTime completedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<ItemDto> items = new ArrayList<>();
    private List<MaterialDto> materials = new ArrayList<>();
    private List<ServiceAttachmentDto> attachments = new ArrayList<>();
    private List<StatusHistoryDto> statusHistory = new ArrayList<>();
    private List<AdvanceDto> advances = new ArrayList<>();

    public ServiceOrderDto() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UUID getAssignedUserId() {
        return assignedUserId;
    }

    public void setAssignedUserId(UUID assignedUserId) {
        this.assignedUserId = assignedUserId;
    }

    public BigDecimal getLaborAmount() {
        return laborAmount;
    }

    public void setLaborAmount(BigDecimal laborAmount) {
        this.laborAmount = laborAmount;
    }

    public BigDecimal getEstimatedTotal() {
        return estimatedTotal;
    }

    public void setEstimatedTotal(BigDecimal estimatedTotal) {
        this.estimatedTotal = estimatedTotal;
    }

    public BigDecimal getAdvancesTotal() {
        return advancesTotal;
    }

    public void setAdvancesTotal(BigDecimal advancesTotal) {
        this.advancesTotal = advancesTotal;
    }

    public BigDecimal getRemainingBalance() {
        return remainingBalance;
    }

    public void setRemainingBalance(BigDecimal remainingBalance) {
        this.remainingBalance = remainingBalance;
    }

    public OffsetDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(OffsetDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<ItemDto> getItems() {
        return items;
    }

    public void setItems(List<ItemDto> items) {
        this.items = items;
    }

    public List<MaterialDto> getMaterials() {
        return materials;
    }

    public void setMaterials(List<MaterialDto> materials) {
        this.materials = materials;
    }

    public List<ServiceAttachmentDto> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<ServiceAttachmentDto> attachments) {
        this.attachments = attachments;
    }

    public List<StatusHistoryDto> getStatusHistory() {
        return statusHistory;
    }

    public void setStatusHistory(List<StatusHistoryDto> statusHistory) {
        this.statusHistory = statusHistory;
    }

    public List<AdvanceDto> getAdvances() {
        return advances;
    }

    public void setAdvances(List<AdvanceDto> advances) {
        this.advances = advances;
    }

    public static class ItemDto {

        private UUID id;
        private String description;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;

        public ItemDto() {
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
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

        public BigDecimal getLineTotal() {
            return lineTotal;
        }

        public void setLineTotal(BigDecimal lineTotal) {
            this.lineTotal = lineTotal;
        }
    }

    public static class MaterialDto {

        private UUID id;
        private UUID variantId;
        private BigDecimal plannedQuantity;
        private BigDecimal consumedQuantity;

        public MaterialDto() {
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
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

        public BigDecimal getConsumedQuantity() {
            return consumedQuantity;
        }

        public void setConsumedQuantity(BigDecimal consumedQuantity) {
            this.consumedQuantity = consumedQuantity;
        }
    }

    public static class StatusHistoryDto {

        private UUID id;
        private String oldStatus;
        private String newStatus;
        private UUID actorId;
        private OffsetDateTime changedAt;
        private String notes;

        public StatusHistoryDto() {
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getOldStatus() {
            return oldStatus;
        }

        public void setOldStatus(String oldStatus) {
            this.oldStatus = oldStatus;
        }

        public String getNewStatus() {
            return newStatus;
        }

        public void setNewStatus(String newStatus) {
            this.newStatus = newStatus;
        }

        public UUID getActorId() {
            return actorId;
        }

        public void setActorId(UUID actorId) {
            this.actorId = actorId;
        }

        public OffsetDateTime getChangedAt() {
            return changedAt;
        }

        public void setChangedAt(OffsetDateTime changedAt) {
            this.changedAt = changedAt;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }

    public static class AdvanceDto {

        private UUID id;
        private UUID cashSessionId;
        private BigDecimal amount;
        private BigDecimal appliedAmount;
        private String paymentMethod;
        private String reference;
        private OffsetDateTime createdAt;

        public AdvanceDto() {
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public UUID getCashSessionId() {
            return cashSessionId;
        }

        public void setCashSessionId(UUID cashSessionId) {
            this.cashSessionId = cashSessionId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public BigDecimal getAppliedAmount() {
            return appliedAmount;
        }

        public void setAppliedAmount(BigDecimal appliedAmount) {
            this.appliedAmount = appliedAmount;
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

        public OffsetDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(OffsetDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }
}
