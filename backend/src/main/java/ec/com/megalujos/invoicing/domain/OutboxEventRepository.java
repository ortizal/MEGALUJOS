package ec.com.megalujos.invoicing.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Eventos vencidos: PENDING con next_attempt_at <= now, más eventos
     * PROCESSING cuyo locked_until expiró (recuperación tras caída del worker).
     */
    @Query("""
            SELECT e FROM OutboxEvent e
            WHERE (e.status = 'PENDING' AND e.nextAttemptAt <= :now)
               OR (e.status = 'PROCESSING' AND e.lockedUntil IS NOT NULL AND e.lockedUntil <= :now)
            ORDER BY e.createdAt ASC
            """)
    List<OutboxEvent> findDueEvents(@Param("now") OffsetDateTime now);

    /** Reclamo atómico del evento: solo un worker pasa a PROCESSING (con locked_until y attempts+1). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE OutboxEvent e
            SET e.status = 'PROCESSING', e.lockedUntil = :lockedUntil, e.attempts = e.attempts + 1
            WHERE e.id = :id
              AND (e.status = 'PENDING'
                   OR (e.status = 'PROCESSING' AND (e.lockedUntil IS NULL OR e.lockedUntil <= :now)))
            """)
    int claimEvent(@Param("id") UUID id, @Param("lockedUntil") OffsetDateTime lockedUntil,
                   @Param("now") OffsetDateTime now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE OutboxEvent e
            SET e.status = 'PENDING', e.nextAttemptAt = :nextAttemptAt, e.errorMessage = :errorMessage, e.lockedUntil = null
            WHERE e.id = :id
            """)
    int rescheduleEvent(@Param("id") UUID id, @Param("nextAttemptAt") OffsetDateTime nextAttemptAt,
                        @Param("errorMessage") String errorMessage);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE OutboxEvent e
            SET e.status = :status, e.errorMessage = :errorMessage, e.lockedUntil = null
            WHERE e.id = :id
            """)
    int finishEvent(@Param("id") UUID id, @Param("status") String status, @Param("errorMessage") String errorMessage);
}
