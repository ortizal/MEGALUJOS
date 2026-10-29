package ec.com.megalujos.cash.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Arqueo por medio de pago de una sesión de caja.
 * Consulta nativa sobre sale_payments/sales sin ampliar los repositorios del módulo pos.
 * Excluye APPLIED_ADVANCE porque no representa dinero físico en la caja.
 */
@Repository
public interface CashReconciliationRepository extends JpaRepository<CashSession, UUID> {

    @Query(value = """
            SELECT sp.method AS method, COALESCE(SUM(sp.amount), 0) AS total
            FROM sale_payments sp
            INNER JOIN sales s ON s.id = sp.sale_id
            WHERE s.cash_session_id = :sessionId
              AND sp.method <> 'APPLIED_ADVANCE'
            GROUP BY sp.method
            ORDER BY sp.method
            """, nativeQuery = true)
    List<Object[]> sumPaymentsByMethodForSession(@Param("sessionId") UUID sessionId);
}
