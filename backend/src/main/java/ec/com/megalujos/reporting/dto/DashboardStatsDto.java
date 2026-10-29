package ec.com.megalujos.reporting.dto;

import java.math.BigDecimal;

public class DashboardStatsDto {

    private long totalSalesTodayCount;
    private BigDecimal totalSalesTodayAmount;
    private long lowStockProductsCount;
    private long openCashSessionsCount;
    private BigDecimal currentCashSessionBalance;

    public DashboardStatsDto() {
    }

    public long getTotalSalesTodayCount() {
        return totalSalesTodayCount;
    }

    public void setTotalSalesTodayCount(long totalSalesTodayCount) {
        this.totalSalesTodayCount = totalSalesTodayCount;
    }

    public BigDecimal getTotalSalesTodayAmount() {
        return totalSalesTodayAmount;
    }

    public void setTotalSalesTodayAmount(BigDecimal totalSalesTodayAmount) {
        this.totalSalesTodayAmount = totalSalesTodayAmount;
    }

    public long getLowStockProductsCount() {
        return lowStockProductsCount;
    }

    public void setLowStockProductsCount(long lowStockProductsCount) {
        this.lowStockProductsCount = lowStockProductsCount;
    }

    public long getOpenCashSessionsCount() {
        return openCashSessionsCount;
    }

    public void setOpenCashSessionsCount(long openCashSessionsCount) {
        this.openCashSessionsCount = openCashSessionsCount;
    }

    public BigDecimal getCurrentCashSessionBalance() {
        return currentCashSessionBalance;
    }

    public void setCurrentCashSessionBalance(BigDecimal currentCashSessionBalance) {
        this.currentCashSessionBalance = currentCashSessionBalance;
    }
}
