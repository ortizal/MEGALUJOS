package ec.com.megalujos.inventory.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AdjustmentResponseDto {

    private UUID id;
    private String adjustmentNumber;
    private UUID warehouseId;
    private String status;
    private String reason;
    private UUID createdBy;
    private UUID approvedBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime approvedAt;
    private List<ItemResponseDto> items = new ArrayList<>();

    public AdjustmentResponseDto() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getAdjustmentNumber() {
        return adjustmentNumber;
    }

    public void setAdjustmentNumber(String adjustmentNumber) {
        this.adjustmentNumber = adjustmentNumber;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
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
        private BigDecimal currentQuantity;
        private BigDecimal adjustedQuantity;
        private BigDecimal difference;
        private BigDecimal cost;

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

        public BigDecimal getCurrentQuantity() {
            return currentQuantity;
        }

        public void setCurrentQuantity(BigDecimal currentQuantity) {
            this.currentQuantity = currentQuantity;
        }

        public BigDecimal getAdjustedQuantity() {
            return adjustedQuantity;
        }

        public void setAdjustedQuantity(BigDecimal adjustedQuantity) {
            this.adjustedQuantity = adjustedQuantity;
        }

        public BigDecimal getDifference() {
            return difference;
        }

        public void setDifference(BigDecimal difference) {
            this.difference = difference;
        }

        public BigDecimal getCost() {
            return cost;
        }

        public void setCost(BigDecimal cost) {
            this.cost = cost;
        }
    }
}
