package ec.com.megalujos.cash.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface CashMovementRepository extends JpaRepository<CashMovement, UUID> {
    List<CashMovement> findBySessionIdOrderByCreatedAtDesc(UUID sessionId);

    @Query("SELECT COALESCE(SUM(cm.amount), 0) FROM CashMovement cm WHERE cm.sessionId = :sessionId AND cm.movementType IN ('SALE_PAYMENT', 'ADVANCE_PAYMENT', 'INCOME')")
    BigDecimal sumIncomeBySessionId(@Param("sessionId") UUID sessionId);

    @Query("SELECT COALESCE(SUM(cm.amount), 0) FROM CashMovement cm WHERE cm.sessionId = :sessionId AND cm.movementType IN ('EXPENSE', 'REFUND')")
    BigDecimal sumExpenseBySessionId(@Param("sessionId") UUID sessionId);
}
