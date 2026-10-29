package ec.com.megalujos.identity.api;

import ec.com.megalujos.common.audit.AuditLog;
import ec.com.megalujos.common.audit.AuditService;
import ec.com.megalujos.common.web.PageResponse;
import ec.com.megalujos.identity.dto.AuditLogResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit")
@Tag(name = "Auditoría", description = "Consulta del registro de auditoría del sistema")
@PreAuthorize("hasAuthority('audit:read') or hasRole('ADMIN')")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @Operation(summary = "Consultar auditoría", description = "Pagina los registros de auditoría filtrando por actor y tipo de entidad")
    public ResponseEntity<PageResponse<AuditLogResponse>> search(
            @RequestParam(value = "actorId", required = false) UUID actorId,
            @RequestParam(value = "entityType", required = false) String entityType,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AuditLog> page = auditService.search(actorId, entityType, pageable);
        PageResponse<AuditLogResponse> response = PageResponse.from(page, this::toResponse);
        return ResponseEntity.ok(response);
    }

    private AuditLogResponse toResponse(AuditLog log) {
        AuditLogResponse response = new AuditLogResponse();
        response.setId(log.getId());
        response.setActorId(log.getActorId());
        response.setAction(log.getAction());
        response.setEntityType(log.getEntityType());
        response.setEntityId(log.getEntityId());
        response.setMetadata(log.getMetadata());
        response.setIpAddress(log.getIpAddress());
        response.setOccurredAt(log.getOccurredAt());
        return response;
    }
}
