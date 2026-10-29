package ec.com.megalujos.common.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /** Escritura no bloqueante: nunca debe romper la operación principal. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID actorId, String action, String entityType, UUID entityId, Object metadata, String ipAddress) {
        try {
            String json = metadata != null ? objectMapper.writeValueAsString(metadata) : "{}";
            repository.save(new AuditLog(actorId, action, entityType, entityId, json, ipAddress));
        } catch (Exception ex) {
            log.error("No se pudo registrar auditoría para {}#{}: {}", entityType, entityId, ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> search(UUID actorId, String entityType, Pageable pageable) {
        return repository.search(actorId, entityType, pageable);
    }
}
