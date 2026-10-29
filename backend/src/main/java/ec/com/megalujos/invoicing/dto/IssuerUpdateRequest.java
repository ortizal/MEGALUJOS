package ec.com.megalujos.invoicing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class IssuerUpdateRequest {

    @NotBlank
    @Size(max = 13)
    private String taxId;

    @NotBlank
    @Size(max = 255)
    private String legalName;

    @Size(max = 255)
    private String tradeName;

    @NotBlank
    @Size(max = 3)
    private String establishmentCode;

    @NotBlank
    @Size(max = 3)
    private String emissionPointCode;

    @NotBlank
    @Pattern(regexp = "TEST|PRODUCTION", message = "environment debe ser TEST o PRODUCTION")
    private String environment;

    @Size(max = 2000)
    private String mainAddress;

    public IssuerUpdateRequest() {
    }

    public String getTaxId() {
        return taxId;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    public String getLegalName() {
        return legalName;
    }

    public void setLegalName(String legalName) {
        this.legalName = legalName;
    }

    public String getTradeName() {
        return tradeName;
    }

    public void setTradeName(String tradeName) {
        this.tradeName = tradeName;
    }

    public String getEstablishmentCode() {
        return establishmentCode;
    }

    public void setEstablishmentCode(String establishmentCode) {
        this.establishmentCode = establishmentCode;
    }

    public String getEmissionPointCode() {
        return emissionPointCode;
    }

    public void setEmissionPointCode(String emissionPointCode) {
        this.emissionPointCode = emissionPointCode;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getMainAddress() {
        return mainAddress;
    }

    public void setMainAddress(String mainAddress) {
        this.mainAddress = mainAddress;
    }
}
