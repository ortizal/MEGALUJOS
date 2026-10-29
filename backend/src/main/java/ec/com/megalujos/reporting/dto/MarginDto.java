package ec.com.megalujos.reporting.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Margen bruto ESTIMADO (no contable): usa el costPrice actual de la variante. */
public class MarginDto {

    private UUID variantId;
    private String sku;
    private String productName;
    private BigDecimal quantity = BigDecimal.ZERO;
    private BigDecimal revenue = BigDecimal.ZERO;
    private BigDecimal cost = BigDecimal.ZERO;
    private BigDecimal estimatedMargin = BigDecimal.ZERO;

    public MarginDto() {
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

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public BigDecimal getEstimatedMargin() {
        return estimatedMargin;
    }

    public void setEstimatedMargin(BigDecimal estimatedMargin) {
        this.estimatedMargin = estimatedMargin;
    }
}
