package ec.com.megalujos.reporting.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class StockAlertDto {

    private UUID variantId;
    private String sku;
    private String productName;
    private BigDecimal quantity = BigDecimal.ZERO;
    private BigDecimal minStock = BigDecimal.ZERO;

    public StockAlertDto() {
    }

    public UUID getVariantId() {
        return variantId;
    }

    public void setVariantId(UUID variantId) {
        this.variantId = variantId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getMinStock() {
        return minStock;
    }

    public void setMinStock(BigDecimal minStock) {
        this.minStock = minStock;
    }
}
