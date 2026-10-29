package ec.com.megalujos.common.tax;

import ec.com.megalujos.common.exception.BusinessRuleException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Resuelve las tasas tributarias desde la base de datos (tax_rules).
 * Nunca fija tasas en código: la normativa se configura y versiona por fechas.
 */
@Service
public class TaxService {

    private static final ZoneId ECUADOR = ZoneId.of("America/Guayaquil");

    private final TaxRuleRepository taxRuleRepository;

    public TaxService(TaxRuleRepository taxRuleRepository) {
        this.taxRuleRepository = taxRuleRepository;
    }

    @Transactional(readOnly = true)
    public TaxRule resolveDefault() {
        return resolveDefault(LocalDate.now(ECUADOR));
    }

    @Transactional(readOnly = true)
    public TaxRule resolveDefault(LocalDate date) {
        return taxRuleRepository.findDefaultValid(date)
                .orElseThrow(() -> new BusinessRuleException("NO_TAX_RULE",
                        "No existe una regla tributaria vigente configurada"));
    }

    @Transactional(readOnly = true)
    public TaxRule resolveByCode(String code, LocalDate date) {
        Optional<TaxRule> rule = taxRuleRepository.findValidByCode(code, date);
        if (rule.isPresent()) {
            return rule.get();
        }
        if (code == null || code.isBlank()) {
            return resolveDefault(date);
        }
        throw new BusinessRuleException("TAX_RULE_NOT_FOUND",
                "No existe regla tributaria vigente para el código " + code);
    }

    @Transactional(readOnly = true)
    public List<TaxRule> listActive() {
        LocalDate today = LocalDate.now(ECUADOR);
        return taxRuleRepository.findAll().stream()
                .filter(t -> t.isValidOn(today))
                .toList();
    }

    public BigDecimal taxAmount(BigDecimal taxableBase, BigDecimal rate) {
        if (taxableBase == null || rate == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return taxableBase.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal computeLineTax(UUID ignoredVariantId, BigDecimal taxableBase, String taxCode, LocalDate date) {
        TaxRule rule = resolveByCode(taxCode, date);
        return taxAmount(taxableBase, rule.getRate());
    }
}
