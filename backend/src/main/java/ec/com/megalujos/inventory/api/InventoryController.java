package ec.com.megalujos.inventory.api;

import ec.com.megalujos.common.web.PageResponse;
import ec.com.megalujos.inventory.application.InventoryService;
import ec.com.megalujos.inventory.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import ec.com.megalujos.common.security.UserPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
@Tag(name = "Inventario", description = "Existencias, kardex, ajustes, conteos físicos y alertas de stock mínimo")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/balances")
    @PreAuthorize("hasAuthority('inventory:read') or hasRole('ADMIN')")
    @Operation(summary = "Listar existencias por variante y bodega", description = "Consulta paginada de saldos con filtros por nombre/sku de producto, bodega y solo stock bajo")
    public ResponseEntity<PageResponse<StockBalanceDto>> getBalances(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID warehouseId,
            @RequestParam(required = false, defaultValue = "false") boolean lowStockOnly,
            @PageableDefault(size = 50) Pageable pageable
    ) {
        return ResponseEntity.ok(PageResponse.from(inventoryService.listBalances(q, warehouseId, lowStockOnly, pageable)));
    }

    @GetMapping("/movements")
    @PreAuthorize("hasAuthority('inventory:read') or hasRole('ADMIN')")
    @Operation(summary = "Kardex paginado", description = "Historial inmutable de movimientos de stock con filtros por variante, bodega, tipo y rango de fechas")
    public ResponseEntity<PageResponse<StockMovementDto>> getMovements(
            @RequestParam(required = false) UUID variantId,
            @RequestParam(required = false) UUID warehouseId,
            @RequestParam(required = false) String movementType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateTo,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(PageResponse.from(
                inventoryService.listMovements(variantId, warehouseId, movementType, dateFrom, dateTo, pageable)));
    }

    @PostMapping("/adjustments")
    @PreAuthorize("hasAuthority('inventory:adjust') or hasRole('ADMIN')")
    @Operation(summary = "Crear ajuste de inventario (borrador)", description = "Registra un ajuste en DRAFT con cantidad actual y diferencia calculadas desde el saldo de la bodega")
    public ResponseEntity<AdjustmentResponseDto> createAdjustment(
            @Valid @RequestBody CreateAdjustmentRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AdjustmentResponseDto response = inventoryService.createAdjustment(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/adjustments/{id}/approve")
    @PreAuthorize("hasAuthority('inventory:adjust') or hasRole('ADMIN')")
    @Operation(summary = "Aprobar ajuste de inventario", description = "Aplica las diferencias del ajuste sobre el stock mediante movimientos ADJUSTMENT_IN/ADJUSTMENT_OUT y audita la aprobación")
    public ResponseEntity<AdjustmentResponseDto> approveAdjustment(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(inventoryService.approveAdjustment(id, principal.getId()));
    }

    @GetMapping("/adjustments")
    @PreAuthorize("hasAuthority('inventory:read') or hasRole('ADMIN')")
    @Operation(summary = "Listar ajustes de inventario", description = "Lista paginada de ajustes ordenada por fecha de creación descendente")
    public ResponseEntity<PageResponse<AdjustmentResponseDto>> getAdjustments(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(PageResponse.from(inventoryService.listAdjustments(pageable)));
    }

    @GetMapping("/adjustments/{id}")
    @PreAuthorize("hasAuthority('inventory:read') or hasRole('ADMIN')")
    @Operation(summary = "Obtener detalle de un ajuste", description = "Consulta un ajuste con sus ítems, cantidades y diferencias")
    public ResponseEntity<AdjustmentResponseDto> getAdjustment(@PathVariable UUID id) {
        return ResponseEntity.ok(inventoryService.getAdjustment(id));
    }

    @GetMapping("/counts")
    @PreAuthorize("hasAuthority('inventory:read') or hasRole('ADMIN')")
    @Operation(summary = "Listar conteos físicos", description = "Lista paginada de conteos de inventario")
    public ResponseEntity<PageResponse<CountResponseDto>> getCounts(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(PageResponse.from(inventoryService.listCounts(pageable)));
    }

    @PostMapping("/counts")
    @PreAuthorize("hasAuthority('inventory:count') or hasRole('ADMIN')")
    @Operation(summary = "Crear conteo físico de inventario", description = "Abre un conteo en OPEN capturando cantidad sistema y diferencia contra la cantidad contada")
    public ResponseEntity<CountResponseDto> createCount(
            @Valid @RequestBody CreateCountRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        CountResponseDto response = inventoryService.createCount(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/counts/{id}/complete")
    @PreAuthorize("hasAuthority('inventory:count') or hasRole('ADMIN')")
    @Operation(summary = "Completar conteo físico", description = "Pasa el conteo de OPEN a COUNTED dejándolo listo para aprobación")
    public ResponseEntity<CountResponseDto> completeCount(@PathVariable UUID id) {
        return ResponseEntity.ok(inventoryService.completeCount(id));
    }

    @PostMapping("/counts/{id}/approve")
    @PreAuthorize("hasAuthority('inventory:adjust') or hasRole('ADMIN')")
    @Operation(summary = "Aprobar conteo físico", description = "Aplica las diferencias del conteo sobre el stock con sourceType STOCK_COUNT y audita la aprobación")
    public ResponseEntity<CountResponseDto> approveCount(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(inventoryService.approveCount(id, principal.getId()));
    }

    @GetMapping("/counts/{id}")
    @PreAuthorize("hasAuthority('inventory:read') or hasRole('ADMIN')")
    @Operation(summary = "Obtener detalle de un conteo", description = "Consulta un conteo con sus ítems, cantidad sistema, contada y diferencia")
    public ResponseEntity<CountResponseDto> getCount(@PathVariable UUID id) {
        return ResponseEntity.ok(inventoryService.getCount(id));
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasAuthority('inventory:read') or hasRole('ADMIN')")
    @Operation(summary = "Alertas de stock mínimo", description = "Lista de variantes cuyo disponible es menor o igual al stock mínimo configurado")
    public ResponseEntity<List<StockAlertDto>> getAlerts() {
        return ResponseEntity.ok(inventoryService.listAlerts());
    }
}
