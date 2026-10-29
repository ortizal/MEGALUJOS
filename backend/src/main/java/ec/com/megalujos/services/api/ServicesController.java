package ec.com.megalujos.services.api;

import ec.com.megalujos.common.security.UserPrincipal;
import ec.com.megalujos.common.web.PageResponse;
import ec.com.megalujos.pos.dto.SaleResponseDto;
import ec.com.megalujos.services.application.ServiceOrderService;
import ec.com.megalujos.services.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/service-orders")
@Tag(name = "Órdenes de Servicio", description = "Gestión de órdenes de stickers e instalación: estados, materiales, anticipos, cobro y adjuntos")
public class ServicesController {

    private final ServiceOrderService serviceOrderService;

    public ServicesController(ServiceOrderService serviceOrderService) {
        this.serviceOrderService = serviceOrderService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('services:read') or hasRole('ADMIN')")
    @Operation(summary = "Listar órdenes de servicio", description = "Historial paginado con filtros de estado, tipo, técnico y búsqueda (número/descripción)")
    public ResponseEntity<PageResponse<ServiceOrderSummaryDto>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID assignedUserId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(serviceOrderService.list(status, type, q, assignedUserId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('services:read') or hasRole('ADMIN')")
    @Operation(summary = "Detalle de orden de servicio", description = "Incluye ítems, materiales (planificado/consumido), adjuntos, historial de estados, anticipos y saldo restante")
    public ResponseEntity<ServiceOrderDto> getDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(serviceOrderService.getDetail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('services:manage') or hasRole('ADMIN')")
    @Operation(summary = "Crear orden de servicio", description = "Crea la orden (nace QUOTED o CONFIRMED) con ítems, materiales previstos (sin descontar stock) y archivos")
    public ResponseEntity<ServiceOrderDto> create(
            @Valid @RequestBody CreateServiceOrderRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        ServiceOrderDto response = serviceOrderService.create(request, principal.getId(), httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('services:manage') or hasRole('ADMIN')")
    @Operation(summary = "Actualizar orden de servicio", description = "Actualiza datos generales y recalcula el total estimado con la tarifa tributaria vigente")
    public ResponseEntity<ServiceOrderDto> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateServiceOrderRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(serviceOrderService.update(id, request, principal.getId(), httpRequest.getRemoteAddr()));
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAuthority('services:manage') or hasRole('ADMIN')")
    @Operation(summary = "Cambiar estado de la orden", description = "Valida la transición (QUOTED→CONFIRMED→IN_PROGRESS→READY_FOR_DELIVERY→DELIVERED; CANCELLED desde cualquier estado no entregado), registra historial y auditoría")
    public ResponseEntity<ServiceOrderDto> changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(serviceOrderService.changeStatus(id, request, principal.getId(), httpRequest.getRemoteAddr()));
    }

    @PostMapping("/{id}/materials/{materialId}/consume")
    @PreAuthorize("hasAuthority('services:manage') or hasRole('ADMIN')")
    @Operation(summary = "Consumir material de la orden", description = "Descuenta stock de forma trazable; rechaza si el consumo supera la cantidad planificada")
    public ResponseEntity<ServiceOrderDto> consumeMaterial(
            @PathVariable UUID id,
            @PathVariable UUID materialId,
            @Valid @RequestBody MaterialQuantityRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(serviceOrderService.consumeMaterial(id, materialId, request.getQuantity(), principal.getId()));
    }

    @PostMapping("/{id}/materials/{materialId}/return")
    @PreAuthorize("hasAuthority('services:manage') or hasRole('ADMIN')")
    @Operation(summary = "Devolver material consumido", description = "Reintegra al inventario el material consumido y decrementa el consumo de la orden")
    public ResponseEntity<ServiceOrderDto> returnMaterial(
            @PathVariable UUID id,
            @PathVariable UUID materialId,
            @Valid @RequestBody MaterialQuantityRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(serviceOrderService.returnMaterial(id, materialId, request.getQuantity(), principal.getId()));
    }

    @PostMapping("/{id}/advances")
    @PreAuthorize("hasAuthority('services:manage') or hasRole('ADMIN')")
    @Operation(summary = "Registrar anticipo", description = "Guarda el anticipo de la orden y registra el movimiento ADVANCE_PAYMENT en caja si es en efectivo")
    public ResponseEntity<ServiceOrderDto.AdvanceDto> addAdvance(
            @PathVariable UUID id,
            @Valid @RequestBody CreateAdvanceRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(serviceOrderService.addAdvance(id, request, principal.getId(), httpRequest.getRemoteAddr()));
    }

    @GetMapping("/{id}/advances")
    @PreAuthorize("hasAuthority('services:read') or hasRole('ADMIN')")
    @Operation(summary = "Listar anticipos de la orden", description = "Anticipos recibidos con monto, medio de pago y monto ya aplicado")
    public ResponseEntity<List<ServiceOrderDto.AdvanceDto>> listAdvances(@PathVariable UUID id) {
        return ResponseEntity.ok(serviceOrderService.listAdvances(id));
    }

    @PostMapping("/{id}/charge")
    @PreAuthorize("hasAuthority('sales:create') or hasRole('ADMIN')")
    @Operation(summary = "Cobrar saldo de la orden", description = "Crea la venta del saldo aplicando anticipos una sola vez (APPLIED_ADVANCE) y deja la orden READY_FOR_DELIVERY")
    public ResponseEntity<SaleResponseDto> charge(
            @PathVariable UUID id,
            @Parameter(description = "Clave de idempotencia (UUID) opcional para reintentos seguros")
            @RequestHeader(value = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody ChargeServiceOrderRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        SaleResponseDto response = serviceOrderService.charge(id, request, idempotencyKey, principal.getId(),
                httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/attachments")
    @PreAuthorize("hasAuthority('services:manage') or hasRole('ADMIN')")
    @Operation(summary = "Asociar archivo a la orden", description = "Vincula un archivo ya subido (MediaController) a la orden con un tipo de adjunto")
    public ResponseEntity<ServiceAttachmentDto> addAttachment(
            @PathVariable UUID id,
            @Valid @RequestBody AttachMediaRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceOrderService.addAttachment(id, request));
    }

    @DeleteMapping("/{id}/attachments/{mediaId}")
    @PreAuthorize("hasAuthority('services:manage') or hasRole('ADMIN')")
    @Operation(summary = "Desvincular archivo de la orden", description = "Elimina la asociación orden-archivo (no borra el archivo original)")
    public ResponseEntity<Void> removeAttachment(@PathVariable UUID id, @PathVariable UUID mediaId) {
        serviceOrderService.removeAttachment(id, mediaId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/attachments")
    @PreAuthorize("hasAuthority('services:read') or hasRole('ADMIN')")
    @Operation(summary = "Listar adjuntos de la orden", description = "Archivos asociados con tipo, nombre original y contenido")
    public ResponseEntity<List<ServiceAttachmentDto>> listAttachments(@PathVariable UUID id) {
        return ResponseEntity.ok(serviceOrderService.listAttachments(id));
    }

    @GetMapping("/{id}/attachments/images")
    @PreAuthorize("hasAuthority('services:read') or hasRole('ADMIN')")
    @Operation(summary = "Listar imágenes de la orden", description = "Solo los adjuntos cuyo contenido es una imagen (diseños, previsualizaciones y fotos)")
    public ResponseEntity<List<ServiceAttachmentDto>> listImages(@PathVariable UUID id) {
        return ResponseEntity.ok(serviceOrderService.listImages(id));
    }
}
