package ec.com.megalujos.invoicing.application;

import ec.com.megalujos.common.audit.AuditService;
import ec.com.megalujos.common.exception.BusinessRuleException;
import ec.com.megalujos.common.exception.ResourceNotFoundException;
import ec.com.megalujos.common.tax.TaxService;
import ec.com.megalujos.invoicing.domain.Issuer;
import ec.com.megalujos.invoicing.domain.IssuerRepository;
import ec.com.megalujos.invoicing.dto.InvoicingConfigDto;
import ec.com.megalujos.invoicing.dto.IssuerResponseDto;
import ec.com.megalujos.invoicing.dto.IssuerUpdateRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class IssuerService {

    private final IssuerRepository issuerRepository;
    private final TaxService taxService;
    private final AuditService auditService;
    private final boolean sriEnabled;
    private final String sriEnvironment;
    private final String sriReceptionUrl;
    private final String sriAuthorizationUrl;

    public IssuerService(IssuerRepository issuerRepository,
                         TaxService taxService,
                         AuditService auditService,
                         @Value("${megalujos.sri.enabled:false}") boolean sriEnabled,
                         @Value("${megalujos.sri.environment:TEST}") String sriEnvironment,
                         @Value("${megalujos.sri.reception-url:}") String sriReceptionUrl,
                         @Value("${megalujos.sri.authorization-url:}") String sriAuthorizationUrl) {
        this.issuerRepository = issuerRepository;
        this.taxService = taxService;
        this.auditService = auditService;
        this.sriEnabled = sriEnabled;
        this.sriEnvironment = sriEnvironment;
        this.sriReceptionUrl = sriReceptionUrl;
        this.sriAuthorizationUrl = sriAuthorizationUrl;
    }

    @Transactional(readOnly = true)
    public List<IssuerResponseDto> listIssuers() {
        return issuerRepository.findAllByOrderByCreatedAtAsc().stream()
                .map(IssuerResponseDto::from)
                .toList();
    }

    @Transactional
    public IssuerResponseDto updateIssuer(UUID id, IssuerUpdateRequest request, UUID actorId, String ipAddress) {
        Issuer issuer = issuerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Emisor", id));

        if (request.getTaxId().length() != 13) {
            throw new BusinessRuleException("INVALID_TAX_ID", "El RUC del emisor debe tener 13 dígitos");
        }
        if (request.getEstablishmentCode().length() != 3 || request.getEmissionPointCode().length() != 3) {
            throw new BusinessRuleException("INVALID_EMISION_CODES",
                    "El establecimiento y el punto de emisión deben tener 3 dígitos");
        }

        issuer.setTaxId(request.getTaxId());
        issuer.setLegalName(request.getLegalName());
        issuer.setTradeName(request.getTradeName());
        issuer.setEstablishmentCode(request.getEstablishmentCode());
        issuer.setEmissionPointCode(request.getEmissionPointCode());
        issuer.setEnvironment(request.getEnvironment());
        issuer.setMainAddress(request.getMainAddress());
        Issuer saved = issuerRepository.save(issuer);

        auditService.record(actorId, "ISSUER_UPDATED", "Issuer", saved.getId(),
                request, ipAddress);
        return IssuerResponseDto.from(saved);
    }

    @Transactional(readOnly = true)
    public InvoicingConfigDto getConfig() {
        InvoicingConfigDto dto = new InvoicingConfigDto();
        issuerRepository.findFirstByActiveTrue().ifPresent(issuer ->
                dto.setIssuer(IssuerResponseDto.from(issuer)));
        dto.setSriEnvironment(sriEnvironment);
        dto.setSriReceptionUrl(sriReceptionUrl);
        dto.setSriAuthorizationUrl(sriAuthorizationUrl);
        dto.setSriEnabled(sriEnabled);
        dto.setTaxRules(taxService.listActive().stream()
                .map(InvoicingConfigDto.TaxRuleDto::from)
                .toList());
        return dto;
    }
}
