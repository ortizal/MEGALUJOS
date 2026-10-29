package ec.com.megalujos.pos.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Consultas de solo lectura sobre ventas (historial, arqueo de caja y anulaciones).
 * Se mantiene separado de {@link SaleRepository} para no modificar su contrato original.
 */
@Repository
public interface SaleSearchRepository extends JpaRepository<Sale, UUID> {

    @Query("""
            SELECT s FROM Sale s
            WHERE (CAST(:dateFrom AS STRING) IS NULL OR s.createdAt >= :dateFrom)
              AND (CAST(:dateTo AS STRING) IS NULL OR s.createdAt <= :dateTo)
              AND (CAST(:status AS STRING) IS NULL OR s.status = :status)
              AND (CAST(:customerId AS STRING) IS NULL OR s.customerId = :customerId)
              AND (CAST(:cashSessionId AS STRING) IS NULL OR s.cashSessionId = :cashSessionId)
              AND (CAST(:q AS STRING) IS NULL OR s.saleNumber ILIKE CONCAT('%', CAST(:q AS STRING), '%'))
            """)
    Page<Sale> search(@Param("dateFrom") OffsetDateTime dateFrom,
                      @Param("dateTo") OffsetDateTime dateTo,
                      @Param("status") String status,
                      @Param("customerId") UUID customerId,
                      @Param("cashSessionId") UUID cashSessionId,
                      @Param("q") String q,
                      Pageable pageable);

    @Query("SELECT p.method, COALESCE(SUM(p.amount), 0) FROM SalePayment p JOIN p.sale s WHERE s.cashSessionId = :sessionId GROUP BY p.method")
    List<Object[]> sumPaymentsByMethodForSession(@Param("sessionId") UUID sessionId);

    @Query(value = "SELECT COUNT(*) FROM electronic_documents WHERE sale_id = :saleId", nativeQuery = true)
    long countElectronicDocumentsBySale(@Param("saleId") UUID saleId);
}
