package ec.com.megalujos.reporting.dto;

import java.math.BigDecimal;

public class PopularProductDto {

    private String description;
    private BigDecimal quantitySold = BigDecimal.ZERO;
    private BigDecimal revenue = BigDecimal.ZERO;

    public PopularProductDto() {
    }

    public PopularProductDto(String description, BigDecimal quantitySold, BigDecimal revenue) {
        this.description = description;
        this.quantitySold = quantitySold;
        this.revenue = revenue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getQuantitySold() {
        return quantitySold;
    }

    public void setQuantitySold(BigDecimal quantitySold) {
        this.quantitySold = quantitySold;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }
}
