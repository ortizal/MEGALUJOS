package ec.com.megalujos.invoicing.api;

import ec.com.megalujos.common.security.UserPrincipal;
import ec.com.megalujos.invoicing.application.IssuerService;
import ec.com.megalujos.invoicing.dto.IssuerResponseDto;
import ec.com.megalujos.invoicing.dto.IssuerUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/issuers")
@Tag(name = "Emisores SRI", description = "Configuración del emisor de comprobantes electrónicos (RUC, establecimiento, ambiente)")
public class IssuerController {

    private final IssuerService issuerService;

    public IssuerController(IssuerService issuerService) {
        this.issuerService = issuerService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('settings:manage') or hasRole('ADMIN')")
    @Operation(summary = "Listar emisores", description = "Retorna la configuración de los emisores de comprobantes electrónicos")
    public ResponseEntity<List<IssuerResponseDto>> listIssuers() {
        return ResponseEntity.ok(issuerService.listIssuers());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('settings:manage') or hasRole('ADMIN')")
    @Operation(summary = "Actualizar emisor", description = "Actualiza datos tributarios del emisor: RUC, razón social, establecimiento, punto de emisión, ambiente y dirección")
    public ResponseEntity<IssuerResponseDto> updateIssuer(
            @PathVariable UUID id,
            @Valid @RequestBody IssuerUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {
        UUID actorId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(issuerService.updateIssuer(id, request, actorId, httpRequest.getRemoteAddr()));
    }
}
