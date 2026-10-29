package ec.com.megalujos.invoicing.api;

import ec.com.megalujos.invoicing.application.IssuerService;
import ec.com.megalujos.invoicing.dto.InvoicingConfigDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/invoicing")
@Tag(name = "Configuración de Facturación", description = "Configuración vigente de facturación electrónica: emisor, ambientes SRI y reglas tributarias")
public class InvoicingConfigController {

    private final IssuerService issuerService;

    public InvoicingConfigController(IssuerService issuerService) {
        this.issuerService = issuerService;
    }

    @GetMapping("/config")
    @PreAuthorize("hasAuthority('invoicing:read') or hasRole('ADMIN')")
    @Operation(summary = "Configuración de facturación", description = "Retorna reglas tributarias activas, emisor configurado y ambientes SRI")
    public ResponseEntity<InvoicingConfigDto> getConfig() {
        return ResponseEntity.ok(issuerService.getConfig());
    }
}
