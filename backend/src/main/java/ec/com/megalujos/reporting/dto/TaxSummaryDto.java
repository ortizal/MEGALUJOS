package ec.com.megalujos.reporting.dto;

import java.math.BigDecimal;

public class TaxSummaryDto {

    private String taxCode;
    private BigDecimal taxableAmount = BigDecimal.ZERO;
    private BigDecimal taxAmount = BigDecimal.ZERO;

    public TaxSummaryDto() {
    }

    public TaxSummaryDto(String taxCode, BigDecimal taxableAmount, BigDecimal taxAmount) {
        this.taxCode = taxCode;
        this.taxableAmount = taxableAmount;
        this.taxAmount = taxAmount;
    }

    public String getTaxCode() {
        return taxCode;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }

    public BigDecimal getTaxableAmount() {
        return taxableAmount;
    }

    public void setTaxableAmount(BigDecimal taxableAmount) {
        this.taxableAmount = taxableAmount;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }
}
