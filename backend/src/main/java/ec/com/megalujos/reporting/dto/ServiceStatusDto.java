package ec.com.megalujos.reporting.dto;

import java.math.BigDecimal;

public class ServiceStatusDto {

    private String status;
    private long count;
    private BigDecimal total = BigDecimal.ZERO;

    public ServiceStatusDto() {
    }

    public ServiceStatusDto(String status, long count, BigDecimal total) {
        this.status = status;
        this.count = count;
        this.total = total;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
