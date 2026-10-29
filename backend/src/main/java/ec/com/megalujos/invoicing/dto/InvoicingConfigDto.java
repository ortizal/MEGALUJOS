package ec.com.megalujos.invoicing.dto;

import ec.com.megalujos.common.tax.TaxRule;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class InvoicingConfigDto {

    private IssuerResponseDto issuer;
    private String sriEnvironment;
    private String sriReceptionUrl;
    private String sriAuthorizationUrl;
    private boolean sriEnabled;
    private List<TaxRuleDto> taxRules = new ArrayList<>();

    public InvoicingConfigDto() {
    }

    public IssuerResponseDto getIssuer() {
        return issuer;
    }

    public void setIssuer(IssuerResponseDto issuer) {
        this.issuer = issuer;
    }

    public String getSriEnvironment() {
        return sriEnvironment;
    }

    public void setSriEnvironment(String sriEnvironment) {
        this.sriEnvironment = sriEnvironment;
    }

    public String getSriReceptionUrl() {
        return sriReceptionUrl;
    }

    public void setSriReceptionUrl(String sriReceptionUrl) {
        this.sriReceptionUrl = sriReceptionUrl;
    }

    public String getSriAuthorizationUrl() {
        return sriAuthorizationUrl;
    }

    public void setSriAuthorizationUrl(String sriAuthorizationUrl) {
        this.sriAuthorizationUrl = sriAuthorizationUrl;
    }

    public boolean isSriEnabled() {
        return sriEnabled;
    }

    public void setSriEnabled(boolean sriEnabled) {
        this.sriEnabled = sriEnabled;
    }

    public List<TaxRuleDto> getTaxRules() {
        return taxRules;
    }

    public void setTaxRules(List<TaxRuleDto> taxRules) {
        this.taxRules = taxRules;
    }

    public static class TaxRuleDto {
        private String code;
        private String name;
        private BigDecimal rate;
        private LocalDate validFrom;
        private LocalDate validTo;
        private boolean defaultRule;

        public static TaxRuleDto from(TaxRule rule) {
            TaxRuleDto dto = new TaxRuleDto();
            dto.setCode(rule.getCode());
            dto.setName(rule.getName());
            dto.setRate(rule.getRate());
            dto.setValidFrom(rule.getValidFrom());
            dto.setValidTo(rule.getValidTo());
            dto.setDefaultRule(rule.isDefaultRule());
            return dto;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public BigDecimal getRate() {
            return rate;
        }

        public void setRate(BigDecimal rate) {
            this.rate = rate;
        }

        public LocalDate getValidFrom() {
            return validFrom;
        }

        public void setValidFrom(LocalDate validFrom) {
            this.validFrom = validFrom;
        }

        public LocalDate getValidTo() {
            return validTo;
        }

        public void setValidTo(LocalDate validTo) {
            this.validTo = validTo;
        }

        public boolean isDefaultRule() {
            return defaultRule;
        }

        public void setDefaultRule(boolean defaultRule) {
            this.defaultRule = defaultRule;
        }
    }
}
