package ec.com.megalujos.common.tax;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaxRuleRepository extends JpaRepository<TaxRule, UUID> {

    @Query("SELECT t FROM TaxRule t WHERE t.active = true AND t.code = :code " +
           "AND t.validFrom <= :date AND (t.validTo IS NULL OR t.validTo >= :date) " +
           "ORDER BY t.validFrom DESC LIMIT 1")
    Optional<TaxRule> findValidByCode(@Param("code") String code, @Param("date") LocalDate date);

    @Query("SELECT t FROM TaxRule t WHERE t.active = true AND t.defaultRule = true " +
           "AND t.validFrom <= :date AND (t.validTo IS NULL OR t.validTo >= :date) " +
           "ORDER BY t.validFrom DESC LIMIT 1")
    Optional<TaxRule> findDefaultValid(@Param("date") LocalDate date);
}
