package ec.com.megalujos.pos.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SaleReturnDto {

    private UUID id;
    private String returnNumber;
    private UUID saleId;
    private UUID cashSessionId;
    private BigDecimal totalReturned;
    private String reason;
    private String paymentMethod;
    private UUID authorizedBy;
    private OffsetDateTime createdAt;
    private List<SaleReturnItemDto> items = new ArrayList<>();

    public SaleReturnDto() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getReturnNumber() {
        return returnNumber;
    }

    public void setReturnNumber(String returnNumber) {
        this.returnNumber = returnNumber;
    }

    public UUID getSaleId() {
        return saleId;
    }

    public void setSaleId(UUID saleId) {
        this.saleId = saleId;
    }

    public UUID getCashSessionId() {
        return cashSessionId;
    }

    public void setCashSessionId(UUID cashSessionId) {
        this.cashSessionId = cashSessionId;
    }

    public BigDecimal getTotalReturned() {
        return totalReturned;
    }

    public void setTotalReturned(BigDecimal totalReturned) {
        this.totalReturned = totalReturned;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public UUID getAuthorizedBy() {
        return authorizedBy;
    }

    public void setAuthorizedBy(UUID authorizedBy) {
        this.authorizedBy = authorizedBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<SaleReturnItemDto> getItems() {
        return items;
    }

    public void setItems(List<SaleReturnItemDto> items) {
        this.items = items;
    }

    public static class SaleReturnItemDto {

        private UUID id;
        private UUID saleItemId;
        private BigDecimal quantity;
        private BigDecimal returnAmount;
        private Boolean restock;

        public SaleReturnItemDto() {
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
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

        public BigDecimal getReturnAmount() {
            return returnAmount;
        }

        public void setReturnAmount(BigDecimal returnAmount) {
            this.returnAmount = returnAmount;
        }

        public Boolean getRestock() {
            return restock;
        }

        public void setRestock(Boolean restock) {
            this.restock = restock;
        }
    }
}
