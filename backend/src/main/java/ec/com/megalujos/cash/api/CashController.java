package ec.com.megalujos.cash.api;

import ec.com.megalujos.cash.application.CashSessionService;
import ec.com.megalujos.cash.domain.CashMovement;
import ec.com.megalujos.cash.domain.CashRegister;
import ec.com.megalujos.cash.domain.CashSession;
import ec.com.megalujos.cash.dto.CashCloseRequest;
import ec.com.megalujos.cash.dto.CashCountRequest;
import ec.com.megalujos.cash.dto.CashReconciliationDto;
import ec.com.megalujos.cash.dto.CashSessionDto;
import ec.com.megalujos.common.security.UserPrincipal;
import ec.com.megalujos.common.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cash")
@Tag(name = "Caja y Arqueos", description = "Endpoints para apertura, cierre, ingresos, retiros y arqueo de caja")
public class CashController {

    private final CashSessionService cashSessionService;

    public CashController(CashSessionService cashSessionService) {
        this.cashSessionService = cashSessionService;
    }

    @GetMapping("/registers")
    @Operation(summary = "Listar cajas físicas", description = "Obtiene las cajas registradoras activas")
    public ResponseEntity<List<CashRegister>> getRegisters() {
        return ResponseEntity.ok(cashSessionService.getActiveRegisters());
    }

    @GetMapping("/sessions")
    @PreAuthorize("hasAuthority('cash:manage') or hasRole('ADMIN')")
    @Operation(summary = "Listar sesiones de caja", description = "Historial paginado de sesiones de caja con filtro de estado")
    public ResponseEntity<PageResponse<CashSessionDto>> listSessions(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(cashSessionService.listSessions(status, pageable));
    }

    @GetMapping("/sessions/active")
    @Operation(summary = "Obtener sesión abierta", description = "Consulta la sesión de caja actualmente abierta")
    public ResponseEntity<CashSession> getActiveSession(@RequestParam(required = false) UUID registerId) {
        CashSession session = cashSessionService.getActiveSession(registerId);
        return ResponseEntity.ok(session);
    }

    @PostMapping("/sessions/open")
    @PreAuthorize("hasAuthority('cash:manage') or hasRole('ADMIN')")
    @Operation(summary = "Abrir sesión de caja", description = "Inicia una nueva sesión con fondo de caja")
    public ResponseEntity<CashSession> openSession(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        UUID registerId = UUID.fromString(body.get("registerId").toString());
        BigDecimal openingAmount = new BigDecimal(body.get("openingAmount").toString());
        String notes = body.containsKey("notes") && body.get("notes") != null ? body.get("notes").toString() : null;

        CashSession session = cashSessionService.openSession(registerId, openingAmount, principal.getId(), notes,
                httpRequest.getRemoteAddr());
        return ResponseEntity.ok(session);
    }

    @PostMapping("/sessions/{id}/close")
    @PreAuthorize("hasAuthority('cash:manage') or hasRole('ADMIN')")
    @Operation(summary = "Cerrar sesión de caja", description = "Cierra la sesión registrando el arqueo en efectivo, persistiendo los conteos por medio de pago y calculando diferencias")
    public ResponseEntity<CashSession> closeSession(
            @PathVariable UUID id,
            @Valid @RequestBody CashCloseRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        CashSession session = cashSessionService.closeSession(id, request.getCountedAmount(), request.getCounts(),
                principal.getId(), request.getNotes(), httpRequest.getRemoteAddr());
        return ResponseEntity.ok(session);
    }

    @GetMapping("/sessions/{id}/reconciliation")
    @PreAuthorize("hasAuthority('cash:manage') or hasRole('ADMIN')")
    @Operation(summary = "Arqueo por medio de pago", description = "Esperado vs contado por medio de pago de la sesión (ventas, movimientos y fondo de caja)")
    public ResponseEntity<CashReconciliationDto> reconciliation(@PathVariable UUID id) {
        return ResponseEntity.ok(cashSessionService.reconcile(id));
    }

    @PostMapping("/sessions/{id}/counts")
    @PreAuthorize("hasAuthority('cash:manage') or hasRole('ADMIN')")
    @Operation(summary = "Registrar conteo por medio de pago", description = "Guarda el arqueo (cash_counts) y devuelve el cierre calculado con esperado y diferencia por método")
    public ResponseEntity<CashReconciliationDto> recordCounts(
            @PathVariable UUID id,
            @Valid @RequestBody CashCountRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(cashSessionService.recordCounts(id, request.getCounts(), principal.getId(),
                httpRequest.getRemoteAddr()));
    }

    @PostMapping("/sessions/{id}/movements")
    @PreAuthorize("hasAuthority('cash:manage') or hasRole('ADMIN')")
    @Operation(summary = "Registrar ingreso o retiro de caja", description = "Añade un movimiento de entrada o salida no comercial")
    public ResponseEntity<CashMovement> recordMovement(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        String movementType = body.get("movementType").toString();
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String reason = body.get("reason").toString();

        CashMovement movement = cashSessionService.recordMovement(id, movementType, amount, reason, principal.getId());
        return ResponseEntity.ok(movement);
    }

    @GetMapping("/sessions/{id}/movements")
    @Operation(summary = "Historial de movimientos de sesión", description = "Lista los cobros, retiros e ingresos de la sesión de caja")
    public ResponseEntity<List<CashMovement>> getMovements(@PathVariable UUID id) {
        return ResponseEntity.ok(cashSessionService.getSessionMovements(id));
    }
}
