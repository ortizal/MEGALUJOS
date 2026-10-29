package ec.com.megalujos.pos.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SaleRepository extends JpaRepository<Sale, UUID> {
    Optional<Sale> findByIdempotencyKey(UUID idempotencyKey);
    Optional<Sale> findBySaleNumber(String saleNumber);
    Page<Sale> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT COUNT(s) FROM Sale s WHERE s.createdAt >= :since")
    long countSalesSince(@Param("since") OffsetDateTime since);

    @Query("SELECT COALESCE(SUM(s.grandTotal), 0) FROM Sale s WHERE s.status = 'CONFIRMED' AND s.createdAt >= :since")
    BigDecimal sumTotalSalesSince(@Param("since") OffsetDateTime since);
}
