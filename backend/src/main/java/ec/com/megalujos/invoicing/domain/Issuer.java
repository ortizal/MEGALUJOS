package ec.com.megalujos.invoicing.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "issuers")
public class Issuer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "tax_id", nullable = false, length = 13)
    private String taxId;

    @Column(name = "legal_name", nullable = false)
    private String legalName;

    @Column(name = "trade_name")
    private String tradeName;

    @Column(name = "main_address", columnDefinition = "TEXT")
    private String mainAddress;

    @Column(name = "establishment_address", columnDefinition = "TEXT")
    private String establishmentAddress;

    @Column(name = "special_taxpayer_code", length = 40)
    private String specialTaxpayerCode;

    @Column(name = "obliged_accounting", length = 2)
    private String obligedAccounting = "NO";

    @Column(name = "establishment_code", nullable = false, length = 3)
    private String establishmentCode;

    @Column(name = "emission_point_code", nullable = false, length = 3)
    private String emissionPointCode;

    @Column(nullable = false, length = 12)
    private String environment = "TEST"; // TEST, PRODUCTION

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public Issuer() {
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

    public String getMainAddress() {
        return mainAddress;
    }

    public void setMainAddress(String mainAddress) {
        this.mainAddress = mainAddress;
    }

    public String getEstablishmentAddress() {
        return establishmentAddress;
    }

    public void setEstablishmentAddress(String establishmentAddress) {
        this.establishmentAddress = establishmentAddress;
    }

    public String getSpecialTaxpayerCode() {
        return specialTaxpayerCode;
    }

    public void setSpecialTaxpayerCode(String specialTaxpayerCode) {
        this.specialTaxpayerCode = specialTaxpayerCode;
    }

    public String getObligedAccounting() {
        return obligedAccounting;
    }

    public void setObligedAccounting(String obligedAccounting) {
        this.obligedAccounting = obligedAccounting;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
