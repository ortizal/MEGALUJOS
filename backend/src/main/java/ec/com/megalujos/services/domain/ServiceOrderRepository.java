package ec.com.megalujos.services.domain;

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
public interface ServiceOrderRepository extends JpaRepository<ServiceOrder, UUID> {

    Optional<ServiceOrder> findByOrderNumber(String orderNumber);

    @Query("""
            SELECT so FROM ServiceOrder so
            WHERE (CAST(:status AS STRING) IS NULL OR so.status = :status)
              AND (CAST(:type AS STRING) IS NULL OR so.serviceType = :type)
              AND (CAST(:assignedUserId AS STRING) IS NULL OR so.assignedUserId = :assignedUserId)
              AND (CAST(:q AS STRING) IS NULL OR so.orderNumber ILIKE CONCAT('%', CAST(:q AS STRING), '%')
                   OR so.description ILIKE CONCAT('%', CAST(:q AS STRING), '%'))
            """)
    Page<ServiceOrder> search(@Param("status") String status,
                              @Param("type") String type,
                              @Param("assignedUserId") UUID assignedUserId,
                              @Param("q") String q,
                              Pageable pageable);

    @Query("""
            SELECT so FROM ServiceOrder so
            WHERE so.scheduledAt IS NOT NULL AND so.scheduledAt >= :from AND so.scheduledAt < :to
            """)
    List<ServiceOrder> findScheduledBetween(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);
}
