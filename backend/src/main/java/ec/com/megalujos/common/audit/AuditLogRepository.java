package ec.com.megalujos.common.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findAllByOrderByOccurredAtDesc(Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE (CAST(:actorId AS STRING) IS NULL OR a.actorId = :actorId) " +
           "AND (CAST(:entityType AS STRING) IS NULL OR a.entityType = :entityType) " +
           "ORDER BY a.occurredAt DESC")
    Page<AuditLog> search(@Param("actorId") UUID actorId,
                          @Param("entityType") String entityType,
                          Pageable pageable);
}
