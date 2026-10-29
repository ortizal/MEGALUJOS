package ec.com.megalujos.common.print.api;

import ec.com.megalujos.common.print.application.PrintJobService;
import ec.com.megalujos.common.print.dto.PrintJobRequest;
import ec.com.megalujos.common.print.dto.PrintJobResponseDto;
import ec.com.megalujos.common.security.UserPrincipal;
import ec.com.megalujos.common.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/print-jobs")
@Tag(name = "Cola de Impresión", description = "Trabajos de impresión: tickets térmicos, apertura/cierre de caja, órdenes, etiquetas y RIDE")
public class PrintJobController {

    private final PrintJobService printJobService;

    public PrintJobController(PrintJobService printJobService) {
        this.printJobService = printJobService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('print:manage') or hasRole('ADMIN')")
    @Operation(summary = "Crear trabajo de impresión", description = "Registra un trabajo PENDING; para SALE_TICKET sin payload se rellena automáticamente con los datos de la venta")
    public ResponseEntity<PrintJobResponseDto> create(
            @Valid @RequestBody PrintJobRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {
        UUID actorId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(printJobService.create(request, actorId, httpRequest.getRemoteAddr()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('print:manage') or hasRole('ADMIN')")
    @Operation(summary = "Listar trabajos de impresión", description = "Lista paginada; filtro opcional por status")
    public ResponseEntity<PageResponse<PrintJobResponseDto>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(printJobService.list(status, page, size));
    }

    @PostMapping("/{id}/printed")
    @PreAuthorize("hasAuthority('print:manage') or hasRole('ADMIN')")
    @Operation(summary = "Marcar como impreso", description = "Cambia el estado del trabajo a PRINTED y registra auditoría PRINT_JOB")
    public ResponseEntity<PrintJobResponseDto> markPrinted(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {
        UUID actorId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(printJobService.markPrinted(id, actorId, httpRequest.getRemoteAddr()));
    }
}
