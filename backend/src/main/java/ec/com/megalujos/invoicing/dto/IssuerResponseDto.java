package ec.com.megalujos.invoicing.dto;

import ec.com.megalujos.invoicing.domain.Issuer;

import java.util.UUID;

public class IssuerResponseDto {

    private UUID id;
    private UUID branchId;
    private String taxId;
    private String legalName;
    private String tradeName;
    private String establishmentCode;
    private String emissionPointCode;
    private String environment;
    private String mainAddress;
    private boolean active;

    public IssuerResponseDto() {
    }

    public static IssuerResponseDto from(Issuer issuer) {
        IssuerResponseDto dto = new IssuerResponseDto();
        dto.setId(issuer.getId());
        dto.setBranchId(issuer.getBranchId());
        dto.setTaxId(issuer.getTaxId());
        dto.setLegalName(issuer.getLegalName());
        dto.setTradeName(issuer.getTradeName());
        dto.setEstablishmentCode(issuer.getEstablishmentCode());
        dto.setEmissionPointCode(issuer.getEmissionPointCode());
        dto.setEnvironment(issuer.getEnvironment());
        dto.setMainAddress(issuer.getMainAddress());
        dto.setActive(issuer.isActive());
        return dto;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public void setBranchId(UUID branchId) {
        this.branchId = branchId;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
