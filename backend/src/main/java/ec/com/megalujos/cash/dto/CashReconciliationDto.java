package ec.com.megalujos.cash.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Arqueo de caja por medio de pago: esperado vs contado y diferencia. */
public class CashReconciliationDto {

    private UUID sessionId;
    private String status;
    private BigDecimal openingAmount = BigDecimal.ZERO;
    private List<MethodLineDto> lines = new ArrayList<>();
    private BigDecimal totalExpected = BigDecimal.ZERO;
    private BigDecimal totalCounted = BigDecimal.ZERO;
    private BigDecimal totalDifference = BigDecimal.ZERO;
    private OffsetDateTime countedAt;

    public CashReconciliationDto() {
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getOpeningAmount() {
        return openingAmount;
    }

    public void setOpeningAmount(BigDecimal openingAmount) {
        this.openingAmount = openingAmount;
    }

    public List<MethodLineDto> getLines() {
        return lines;
    }

    public void setLines(List<MethodLineDto> lines) {
        this.lines = lines;
    }

    public BigDecimal getTotalExpected() {
        return totalExpected;
    }

    public void setTotalExpected(BigDecimal totalExpected) {
        this.totalExpected = totalExpected;
    }

    public BigDecimal getTotalCounted() {
        return totalCounted;
    }

    public void setTotalCounted(BigDecimal totalCounted) {
        this.totalCounted = totalCounted;
    }

    public BigDecimal getTotalDifference() {
        return totalDifference;
    }

    public void setTotalDifference(BigDecimal totalDifference) {
        this.totalDifference = totalDifference;
    }

    public OffsetDateTime getCountedAt() {
        return countedAt;
    }

    public void setCountedAt(OffsetDateTime countedAt) {
        this.countedAt = countedAt;
    }

    public static class MethodLineDto {

        private String method;
        private BigDecimal expected = BigDecimal.ZERO;
        private BigDecimal counted;
        private BigDecimal difference;

        public MethodLineDto() {
        }

        public MethodLineDto(String method, BigDecimal expected, BigDecimal counted, BigDecimal difference) {
            this.method = method;
            this.expected = expected;
            this.counted = counted;
            this.difference = difference;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public BigDecimal getExpected() {
            return expected;
        }

        public void setExpected(BigDecimal expected) {
            this.expected = expected;
        }

        public BigDecimal getCounted() {
            return counted;
        }

        public void setCounted(BigDecimal counted) {
            this.counted = counted;
        }

        public BigDecimal getDifference() {
            return difference;
        }

        public void setDifference(BigDecimal difference) {
            this.difference = difference;
        }
    }
}
