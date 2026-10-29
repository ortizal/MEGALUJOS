package ec.com.megalujos.pos.api;

import ec.com.megalujos.common.security.UserPrincipal;
import ec.com.megalujos.common.web.PageResponse;
import ec.com.megalujos.pos.application.SaleReturnService;
import ec.com.megalujos.pos.application.SaleService;
import ec.com.megalujos.pos.dto.ConfirmSaleRequest;
import ec.com.megalujos.pos.dto.CreateReturnRequest;
import ec.com.megalujos.pos.dto.SaleDetailDto;
import ec.com.megalujos.pos.dto.SaleResponseDto;
import ec.com.megalujos.pos.dto.SaleReturnDto;
import ec.com.megalujos.pos.dto.SaleSummaryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sales")
@Tag(name = "Punto de Venta (POS)", description = "Endpoints para registro atómico de ventas, pagos, idempotencia, historial y devoluciones")
public class PosController {

    private final SaleService saleService;
    private final SaleReturnService saleReturnService;

    public PosController(SaleService saleService, SaleReturnService saleReturnService) {
        this.saleService = saleService;
        this.saleReturnService = saleReturnService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('sales:create') or hasRole('ADMIN')")
    @Operation(summary = "Confirmar venta POS", description = "Registra una venta con clave de idempotencia, bloquea y descuenta stock atómicamente, y registra pagos")
    public ResponseEntity<SaleResponseDto> confirmSale(
            @Parameter(description = "Clave de idempotencia única (UUID) para evitar duplicados en reintentos", required = true)
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody ConfirmSaleRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID currentUserId = principal != null ? principal.getId() : null;
        SaleResponseDto response = saleService.confirmSale(request, idempotencyKey, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('sales:read') or hasRole('ADMIN')")
    @Operation(summary = "Historial de ventas", description = "Lista paginada de ventas con filtros de fecha, estado, cliente, número de venta y sesión de caja")
    public ResponseEntity<PageResponse<SaleSummaryDto>> listSales(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateTo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID cashSessionId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(saleReturnService.listSales(dateFrom, dateTo, status, customerId, q, cashSessionId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('sales:read') or hasRole('ADMIN')")
    @Operation(summary = "Obtener detalle de venta", description = "Consulta la venta, desglose de impuestos, pagos, ticket e historial de devoluciones")
    public ResponseEntity<SaleDetailDto> getSaleDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(saleReturnService.getSaleDetail(id));
    }

    @GetMapping("/{id}/returns")
    @PreAuthorize("hasAuthority('sales:read') or hasRole('ADMIN')")
    @Operation(summary = "Historial de devoluciones de una venta", description = "Lista las devoluciones registradas sobre la venta con sus líneas, montos y motivo")
    public ResponseEntity<List<SaleReturnDto>> listReturns(@PathVariable UUID id) {
        return ResponseEntity.ok(saleReturnService.listReturns(id));
    }

    @PostMapping("/{id}/returns")
    @PreAuthorize("hasAuthority('sales:return') or hasRole('ADMIN')")
    @Operation(summary = "Registrar devolución de venta", description = "Devuelve dinero proporcional, reintegra stock (opcional), registra REFUND en caja si es en efectivo y actualiza el estado de la venta")
    public ResponseEntity<SaleReturnDto> createReturn(
            @PathVariable UUID id,
            @Valid @RequestBody CreateReturnRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        SaleReturnDto response = saleReturnService.createReturn(id, request, principal.getId(), httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/void")
    @PreAuthorize("hasAuthority('sales:read') and hasRole('ADMIN')")
    @Operation(summary = "Anular venta", description = "Anula una venta sin comprobantes electrónicos ni devoluciones; reintegra stock si fue confirmada")
    public ResponseEntity<SaleDetailDto> voidSale(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(saleReturnService.voidSale(id, principal.getId(), httpRequest.getRemoteAddr()));
    }
}
