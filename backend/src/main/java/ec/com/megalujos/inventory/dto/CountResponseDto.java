package ec.com.megalujos.inventory.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CountResponseDto {

    private UUID id;
    private String countNumber;
    private UUID warehouseId;
    private String status;
    private String notes;
    private UUID createdBy;
    private UUID approvedBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime completedAt;
    private OffsetDateTime approvedAt;
    private List<ItemResponseDto> items = new ArrayList<>();

    public CountResponseDto() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCountNumber() {
        return countNumber;
    }

    public void setCountNumber(String countNumber) {
        this.countNumber = countNumber;
    }

    public UUID getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(UUID warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(UUID approvedBy) {
        this.approvedBy = approvedBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public OffsetDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(OffsetDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public List<ItemResponseDto> getItems() {
        return items;
    }

    public void setItems(List<ItemResponseDto> items) {
        this.items = items;
    }

    public static class ItemResponseDto {

        private UUID id;
        private UUID variantId;
        private BigDecimal systemQuantity;
        private BigDecimal countedQuantity;
        private BigDecimal difference;

        public ItemResponseDto() {
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

        public BigDecimal getSystemQuantity() {
            return systemQuantity;
        }

        public void setSystemQuantity(BigDecimal systemQuantity) {
            this.systemQuantity = systemQuantity;
        }

        public BigDecimal getCountedQuantity() {
            return countedQuantity;
        }

        public void setCountedQuantity(BigDecimal countedQuantity) {
            this.countedQuantity = countedQuantity;
        }

        public BigDecimal getDifference() {
            return difference;
        }

        public void setDifference(BigDecimal difference) {
            this.difference = difference;
        }
    }
}
