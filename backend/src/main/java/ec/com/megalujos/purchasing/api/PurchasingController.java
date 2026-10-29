package ec.com.megalujos.purchasing.api;

import ec.com.megalujos.common.security.UserPrincipal;
import ec.com.megalujos.common.web.PageResponse;
import ec.com.megalujos.purchasing.application.GoodsReceiptService;
import ec.com.megalujos.purchasing.application.PurchaseOrderService;
import ec.com.megalujos.purchasing.application.SupplierService;
import ec.com.megalujos.purchasing.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Compras", description = "Proveedores, órdenes de compra y recepciones de mercadería")
public class PurchasingController {

    private final SupplierService supplierService;
    private final PurchaseOrderService purchaseOrderService;
    private final GoodsReceiptService goodsReceiptService;

    public PurchasingController(SupplierService supplierService,
                                PurchaseOrderService purchaseOrderService,
                                GoodsReceiptService goodsReceiptService) {
        this.supplierService = supplierService;
        this.purchaseOrderService = purchaseOrderService;
        this.goodsReceiptService = goodsReceiptService;
    }

    @GetMapping("/suppliers")
    @PreAuthorize("hasAuthority('purchasing:manage') or hasRole('ADMIN')")
    @Operation(summary = "Listar proveedores", description = "Lista paginada de proveedores con filtro de texto por nombre o RUC/cédula")
    public ResponseEntity<PageResponse<SupplierResponseDto>> getSuppliers(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(PageResponse.from(supplierService.list(q, pageable)));
    }

    @PostMapping("/suppliers")
    @PreAuthorize("hasAuthority('purchasing:manage') or hasRole('ADMIN')")
    @Operation(summary = "Crear proveedor", description = "Crea un proveedor validando RUC/cédula único")
    public ResponseEntity<SupplierResponseDto> createSupplier(
            @Valid @RequestBody SupplierRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        SupplierResponseDto response = supplierService.create(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/suppliers/{id}")
    @PreAuthorize("hasAuthority('purchasing:manage') or hasRole('ADMIN')")
    @Operation(summary = "Actualizar proveedor", description = "Actualiza un proveedor validando RUC/cédula único")
    public ResponseEntity<SupplierResponseDto> updateSupplier(
            @PathVariable UUID id,
            @Valid @RequestBody SupplierRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(supplierService.update(id, request, principal.getId()));
    }

    @PatchMapping("/suppliers/{id}/active")
    @PreAuthorize("hasAuthority('purchasing:manage') or hasRole('ADMIN')")
    @Operation(summary = "Activar o desactivar proveedor", description = "Cambia el estado activo de un proveedor")
    public ResponseEntity<SupplierResponseDto> setSupplierActive(
            @PathVariable UUID id,
            @RequestParam boolean active,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(supplierService.setActive(id, active, principal.getId()));
    }

    @GetMapping("/purchase-orders")
    @PreAuthorize("hasAuthority('purchasing:manage') or hasRole('ADMIN')")
    @Operation(summary = "Listar órdenes de compra", description = "Lista paginada de órdenes con filtros por estado y texto (número de orden o proveedor)")
    public ResponseEntity<PageResponse<PurchaseOrderResponseDto>> getPurchaseOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "orderedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(PageResponse.from(purchaseOrderService.list(status, q, pageable)));
    }

    @PostMapping("/purchase-orders")
    @PreAuthorize("hasAuthority('purchasing:manage') or hasRole('ADMIN')")
    @Operation(summary = "Crear orden de compra", description = "Crea una orden en DRAFT con numeración OC-yyyymmdd-XXXX y totales calculados en servidor (subtotal, impuesto por defecto y total)")
    public ResponseEntity<PurchaseOrderResponseDto> createPurchaseOrder(
            @Valid @RequestBody CreatePurchaseOrderRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        PurchaseOrderResponseDto response = purchaseOrderService.create(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/purchase-orders/{id}")
    @PreAuthorize("hasAuthority('purchasing:manage') or hasRole('ADMIN')")
    @Operation(summary = "Actualizar orden de compra", description = "Reemplaza proveedor, notas e ítems de una orden solo si está en DRAFT; recalcula totales")
    public ResponseEntity<PurchaseOrderResponseDto> updatePurchaseOrder(
            @PathVariable UUID id,
            @Valid @RequestBody CreatePurchaseOrderRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(purchaseOrderService.update(id, request, principal.getId()));
    }

    @PostMapping("/purchase-orders/{id}/order")
    @PreAuthorize("hasAuthority('purchasing:manage') or hasRole('ADMIN')")
    @Operation(summary = "Enviar orden de compra", description = "Cambia el estado de la orden de DRAFT a ORDERED para su gestión por el proveedor")
    public ResponseEntity<PurchaseOrderResponseDto> orderPurchaseOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(purchaseOrderService.markOrdered(id));
    }

    @GetMapping("/purchase-orders/{id}")
    @PreAuthorize("hasAuthority('purchasing:manage') or hasRole('ADMIN')")
    @Operation(summary = "Detalle de orden de compra", description = "Consulta una orden con sus ítems, SKU, cantidad pedida y cantidad ya recibida")
    public ResponseEntity<PurchaseOrderResponseDto> getPurchaseOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(purchaseOrderService.get(id));
    }

    @PostMapping("/purchase-orders/{id}/receipts")
    @PreAuthorize("hasAuthority('purchasing:manage') or hasRole('ADMIN')")
    @Operation(summary = "Registrar recepción de mercadería", description = "Crea una recepción REC-yyyymmdd-XXXX, valida cantidades contra el pendiente, ingresa stock con costo de la línea y actualiza el estado de la orden")
    public ResponseEntity<GoodsReceiptResponseDto> createReceipt(
            @PathVariable UUID id,
            @Valid @RequestBody CreateGoodsReceiptRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        GoodsReceiptResponseDto response = goodsReceiptService.receive(id, request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/purchase-orders/{id}/receipts")
    @PreAuthorize("hasAuthority('purchasing:manage') or hasRole('ADMIN')")
    @Operation(summary = "Listar recepciones de la orden", description = "Lista paginada de recepciones registradas contra la orden de compra")
    public ResponseEntity<PageResponse<GoodsReceiptResponseDto>> getReceipts(
            @PathVariable UUID id,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(PageResponse.from(goodsReceiptService.listReceipts(id, pageable)));
    }
}
