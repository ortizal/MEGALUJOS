package ec.com.megalujos.invoicing.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ElectronicDocumentRepository extends JpaRepository<ElectronicDocument, UUID> {

    Optional<ElectronicDocument> findByIdempotencyKey(UUID idempotencyKey);

    @Query("SELECT ed FROM ElectronicDocument ed WHERE ed.saleId = :saleId AND ed.documentType = :documentType")
    List<ElectronicDocument> findBySaleIdAndDocumentType(@Param("saleId") UUID saleId,
                                                         @Param("documentType") String documentType);

    /** Facturas electrónicas de una venta aptas para ser referenciadas por nota de crédito. */
    @Query("""
            SELECT ed FROM ElectronicDocument ed
            WHERE ed.saleId = :saleId AND ed.documentType = '01'
              AND ed.status IN :statuses
            ORDER BY ed.createdAt DESC
            """)
    List<ElectronicDocument> findOriginalInvoices(@Param("saleId") UUID saleId,
                                                  @Param("statuses") List<String> statuses);

    @Query("""
            SELECT ed FROM ElectronicDocument ed
            WHERE (CAST(:status AS STRING) IS NULL OR ed.status = :status)
              AND (CAST(:documentType AS STRING) IS NULL OR ed.documentType = :documentType)
              AND (CAST(:dateFrom AS STRING) IS NULL OR ed.createdAt >= :dateFrom)
              AND (CAST(:dateTo AS STRING) IS NULL OR ed.createdAt < :dateTo)
            ORDER BY ed.createdAt DESC
            """)
    Page<ElectronicDocument> search(@Param("status") String status,
                                    @Param("documentType") String documentType,
                                    @Param("dateFrom") OffsetDateTime dateFrom,
                                    @Param("dateTo") OffsetDateTime dateTo,
                                    Pageable pageable);
}
