package ec.com.megalujos.reporting.dto;

import java.math.BigDecimal;

public class SalesByCategoryDto {

    private String category;
    private long count;
    private BigDecimal total = BigDecimal.ZERO;

    public SalesByCategoryDto() {
    }

    public SalesByCategoryDto(String category, long count, BigDecimal total) {
        this.category = category;
        this.count = count;
        this.total = total;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }
}
