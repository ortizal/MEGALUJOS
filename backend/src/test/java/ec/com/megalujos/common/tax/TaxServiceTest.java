package ec.com.megalujos.common.tax;

import ec.com.megalujos.common.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de TaxService: resolución de reglas tributarias y cálculo de impuestos.
 */
@ExtendWith(MockitoExtension.class)
class TaxServiceTest {

    @Mock
    private TaxRuleRepository taxRuleRepository;

    @InjectMocks
    private TaxService taxService;

    // Given: una regla IVA 15 por defecto vigente
    private TaxRule iva15() {
        TaxRule rule = new TaxRule();
        rule.setCode("IVA_15");
        rule.setName("IVA 15%");
        rule.setRate(new BigDecimal("0.15"));
        rule.setDefaultRule(true);
        rule.setActive(true);
        rule.setValidFrom(LocalDate.of(2024, 1, 1));
        return rule;
    }

    @Test
    void resolveDefault_devuelveReglaIva15() {
        // Given
        TaxRule iva = iva15();
        when(taxRuleRepository.findDefaultValid(any(LocalDate.class))).thenReturn(Optional.of(iva));

        // When
        TaxRule resolved = taxService.resolveDefault();

        // Then
        assertNotNull(resolved);
        assertEquals("IVA_15", resolved.getCode());
        assertEquals(0, new BigDecimal("0.15").compareTo(resolved.getRate()));
    }

    @Test
    void resolveDefault_sinReglaVigente_lanzaNoTaxRule() {
        // Given
        when(taxRuleRepository.findDefaultValid(any(LocalDate.class))).thenReturn(Optional.empty());

        // When / Then
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> taxService.resolveDefault());
        assertEquals("NO_TAX_RULE", ex.getCode());
    }

    @Test
    void taxAmount_base100_tasa015_devuelve15() {
        // When: cálculo puro, sin tocar repositorio
        BigDecimal result = taxService.taxAmount(new BigDecimal("100"), new BigDecimal("0.15"));

        // Then
        assertEquals(0, new BigDecimal("15.00").compareTo(result));
        assertEquals(2, result.scale());
    }

    @Test
    void taxAmount_baseONula_devuelveCeroConEscala2() {
        // When
        BigDecimal porBaseNula = taxService.taxAmount(null, new BigDecimal("0.15"));
        BigDecimal porTasaNula = taxService.taxAmount(new BigDecimal("100"), null);

        // Then
        assertEquals(0, BigDecimal.ZERO.compareTo(porBaseNula));
        assertEquals(2, porBaseNula.scale());
        assertEquals(0, BigDecimal.ZERO.compareTo(porTasaNula));
        assertEquals(2, porTasaNula.scale());
    }

    @Test
    void taxAmount_redondeaHalfUpADosDecimales() {
        // 33.333 * 0.15 = 4.99995 -> 5.00 con HALF_UP
        BigDecimal result = taxService.taxAmount(new BigDecimal("33.333"), new BigDecimal("0.15"));
        assertEquals(0, new BigDecimal("5.00").compareTo(result));
    }

    @Test
    void resolveByCode_inexistente_lanzaTaxRuleNotFound() {
        // Given
        LocalDate date = LocalDate.of(2025, 6, 1);
        when(taxRuleRepository.findValidByCode("IVA_99", date)).thenReturn(Optional.empty());

        // When / Then
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> taxService.resolveByCode("IVA_99", date));
        assertEquals("TAX_RULE_NOT_FOUND", ex.getCode());
    }

    @Test
    void resolveByCode_enBlanco_caeAlDefault() {
        // Given: código en blanco -> usa la regla por defecto
        LocalDate date = LocalDate.of(2025, 6, 1);
        TaxRule iva = iva15();
        when(taxRuleRepository.findValidByCode("", date)).thenReturn(Optional.empty());
        when(taxRuleRepository.findDefaultValid(date)).thenReturn(Optional.of(iva));

        // When
        TaxRule resolved = taxService.resolveByCode("", date);

        // Then
        assertEquals("IVA_15", resolved.getCode());
    }

    @Test
    void resolveByCode_existente_devuelveLaRegla() {
        // Given
        LocalDate date = LocalDate.of(2025, 6, 1);
        TaxRule iva = iva15();
        when(taxRuleRepository.findValidByCode("IVA_15", date)).thenReturn(Optional.of(iva));

        // When / Then
        assertEquals(iva, taxService.resolveByCode("IVA_15", date));
    }
}
