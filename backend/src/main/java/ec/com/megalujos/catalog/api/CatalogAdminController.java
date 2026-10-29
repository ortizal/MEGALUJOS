package ec.com.megalujos.catalog.api;

import ec.com.megalujos.catalog.application.BrandAdminService;
import ec.com.megalujos.catalog.application.CategoryAdminService;
import ec.com.megalujos.catalog.application.ProductAdminService;
import ec.com.megalujos.catalog.application.VehicleCatalogService;
import ec.com.megalujos.catalog.dto.*;
import ec.com.megalujos.common.security.UserPrincipal;
import ec.com.megalujos.common.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Catálogo Administrativo", description = "Endpoints administrativos de catálogo: productos, variantes, categorías, marcas, imágenes y compatibilidades de vehículo")
public class CatalogAdminController {

    private final CategoryAdminService categoryAdminService;
    private final BrandAdminService brandAdminService;
    private final ProductAdminService productAdminService;
    private final VehicleCatalogService vehicleCatalogService;

    public CatalogAdminController(
            CategoryAdminService categoryAdminService,
            BrandAdminService brandAdminService,
            ProductAdminService productAdminService,
            VehicleCatalogService vehicleCatalogService
    ) {
        this.categoryAdminService = categoryAdminService;
        this.brandAdminService = brandAdminService;
        this.productAdminService = productAdminService;
        this.vehicleCatalogService = vehicleCatalogService;
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAuthority('catalog:read') or hasRole('ADMIN')")
    @Operation(summary = "Árbol de categorías", description = "Retorna el árbol completo de categorías (incluye inactivas), paginado por categorías raíz")
    public ResponseEntity<PageResponse<CategoryResponse>> getCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(categoryAdminService.getCategoryTree(PageRequest.of(page, size)));
    }

    @PostMapping("/categories")
    @PreAuthorize("hasAuthority('catalog:write') or hasRole('ADMIN')")
    @Operation(summary = "Crear categoría", description = "Crea una categoría; el slug se genera desde el nombre si no se envía y se valida unicidad")
    public ResponseEntity<CategoryResponse> createCategory(
            @Valid @RequestBody CategoryRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(categoryAdminService.create(request, principal.getId(), httpRequest.getRemoteAddr()));
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasAuthority('catalog:write') or hasRole('ADMIN')")
    @Operation(summary = "Actualizar categoría", description = "Actualiza una categoría validando slug único y jerarquía sin ciclos")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(categoryAdminService.update(id, request, principal.getId(), httpRequest.getRemoteAddr()));
    }

    @PatchMapping("/categories/{id}/active")
    @PreAuthorize("hasAuthority('catalog:write') or hasRole('ADMIN')")
    @Operation(summary = "Activar o desactivar categoría", description = "Cambia el estado activo de una categoría")
    public ResponseEntity<CategoryResponse> setCategoryActive(
            @PathVariable UUID id,
            @RequestParam boolean active,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(categoryAdminService.setActive(id, active, principal.getId(), httpRequest.getRemoteAddr()));
    }

    @GetMapping("/brands")
    @PreAuthorize("hasAuthority('catalog:read') or hasRole('ADMIN')")
    @Operation(summary = "Listar marcas", description = "Lista todas las marcas (incluye inactivas) con filtro opcional por texto")
    public ResponseEntity<List<BrandResponse>> getBrands(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(brandAdminService.list(q));
    }

    @PostMapping("/brands")
    @PreAuthorize("hasAuthority('catalog:write') or hasRole('ADMIN')")
    @Operation(summary = "Crear marca", description = "Crea una marca validando nombre único")
    public ResponseEntity<BrandResponse> createBrand(
            @Valid @RequestBody BrandRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(brandAdminService.create(request, principal.getId(), httpRequest.getRemoteAddr()));
    }

    @PutMapping("/brands/{id}")
    @PreAuthorize("hasAuthority('catalog:write') or hasRole('ADMIN')")
    @Operation(summary = "Actualizar marca", description = "Actualiza una marca validando nombre único")
    public ResponseEntity<BrandResponse> updateBrand(
            @PathVariable UUID id,
            @Valid @RequestBody BrandRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(brandAdminService.update(id, request, principal.getId(), httpRequest.getRemoteAddr()));
    }

    @PatchMapping("/brands/{id}/active")
    @PreAuthorize("hasAuthority('catalog:write') or hasRole('ADMIN')")
    @Operation(summary = "Activar o desactivar marca", description = "Cambia el estado activo de una marca")
    public ResponseEntity<BrandResponse> setBrandActive(
            @PathVariable UUID id,
            @RequestParam boolean active,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(brandAdminService.setActive(id, active, principal.getId(), httpRequest.getRemoteAddr()));
    }

    @GetMapping("/products")
    @PreAuthorize("hasAuthority('catalog:read') or hasRole('ADMIN')")
    @Operation(summary = "Listar productos (administrativo)", description = "Lista productos con filtros q, categoryId, brandId, published, active y productType; incluye variantes e imágenes")
    public ResponseEntity<PageResponse<ProductResponse>> getProducts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID brandId,
            @RequestParam(required = false) Boolean published,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String productType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sort
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(sort).ascending());
        return ResponseEntity.ok(productAdminService.list(q, categoryId, brandId, published, active, productType, pageable));
    }

    @GetMapping("/products/{id}")
    @PreAuthorize("hasAuthority('catalog:read') or hasRole('ADMIN')")
    @Operation(summary = "Detalle de producto", description = "Retorna el detalle completo del producto con variantes e imágenes")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable UUID id) {
        return ResponseEntity.ok(productAdminService.get(id));
    }

    @PostMapping("/products")
    @PreAuthorize("hasAuthority('catalog:write') or hasRole('ADMIN')")
    @Operation(summary = "Crear producto", description = "Crea un producto con sus variantes; valida SKU único a nivel de producto y variante")
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductAdminRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(productAdminService.create(request, principal.getId(), httpRequest.getRemoteAddr()));
    }

    @PutMapping("/products/{id}")
    @PreAuthorize("hasAuthority('catalog:write') or hasRole('ADMIN')")
    @Operation(summary = "Actualizar producto", description = "Actualiza cabecera y variantes por id: crea, actualiza o desactiva las no enviadas")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody ProductAdminRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(productAdminService.update(id, request, principal.getId(), httpRequest.getRemoteAddr()));
    }

    @PatchMapping("/products/{id}/publish")
    @PreAuthorize("hasAuthority('catalog:write') or hasRole('ADMIN')")
    @Operation(summary = "Publicar o despublicar producto", description = "Cambia el estado de publicación del producto en la vitrina")
    public ResponseEntity<ProductResponse> setProductPublished(
            @PathVariable UUID id,
            @RequestParam boolean published,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(productAdminService.setPublished(id, published, principal.getId(), httpRequest.getRemoteAddr()));
    }

    @PostMapping("/products/{id}/images")
    @PreAuthorize("hasAuthority('catalog:write') or hasRole('ADMIN')")
    @Operation(summary = "Asociar imagen a producto", description = "Asocia un MediaFile existente al producto; un solo is_primary por producto")
    public ResponseEntity<ProductImageResponse> addProductImage(
            @PathVariable UUID id,
            @RequestParam UUID mediaId,
            @RequestParam(defaultValue = "0") int sortOrder,
            @RequestParam(defaultValue = "false") boolean isPrimary
    ) {
        return ResponseEntity.ok(productAdminService.addImage(id, mediaId, sortOrder, isPrimary));
    }

    @DeleteMapping("/products/{id}/images/{imageId}")
    @PreAuthorize("hasAuthority('catalog:write') or hasRole('ADMIN')")
    @Operation(summary = "Eliminar imagen de producto", description = "Elimina la asociación entre el producto y su imagen")
    public ResponseEntity<Void> removeProductImage(
            @PathVariable UUID id,
            @PathVariable UUID imageId
    ) {
        productAdminService.removeImage(id, imageId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/products/{id}/images/{imageId}/primary")
    @PreAuthorize("hasAuthority('catalog:write') or hasRole('ADMIN')")
    @Operation(summary = "Marcar imagen como principal", description = "Marca la imagen como principal del producto y desmarca las demás")
    public ResponseEntity<ProductImageResponse> setPrimaryProductImage(
            @PathVariable UUID id,
            @PathVariable UUID imageId
    ) {
        return ResponseEntity.ok(productAdminService.setPrimaryImage(id, imageId));
    }

    @GetMapping("/vehicle-brands")
    @PreAuthorize("hasAuthority('catalog:read') or hasRole('ADMIN')")
    @Operation(summary = "Listar marcas de vehículo", description = "Lista todas las marcas de vehículo registradas")
    public ResponseEntity<List<VehicleBrandResponse>> getVehicleBrands() {
        return ResponseEntity.ok(vehicleCatalogService.listBrands());
    }

    @PostMapping("/vehicle-brands")
    @PreAuthorize("hasAuthority('catalog:write') or hasRole('ADMIN')")
    @Operation(summary = "Crear marca de vehículo", description = "Crea una marca de vehículo validando nombre único")
    public ResponseEntity<VehicleBrandResponse> createVehicleBrand(@Valid @RequestBody VehicleBrandRequest request) {
        return ResponseEntity.ok(vehicleCatalogService.createBrand(request));
    }

    @PutMapping("/vehicle-brands/{id}")
    @PreAuthorize("hasAuthority('catalog:write') or hasRole('ADMIN')")
    @Operation(summary = "Actualizar marca de vehículo", description = "Actualiza una marca de vehículo validando nombre único")
    public ResponseEntity<VehicleBrandResponse> updateVehicleBrand(
            @PathVariable UUID id,
            @Valid @RequestBody VehicleBrandRequest request
    ) {
        return ResponseEntity.ok(vehicleCatalogService.updateBrand(id, request));
    }

    @GetMapping("/vehicle-models")
    @PreAuthorize("hasAuthority('catalog:read') or hasRole('ADMIN')")
    @Operation(summary = "Listar modelos de vehículo", description = "Lista modelos de vehículo, filtrables por marca")
    public ResponseEntity<List<VehicleModelResponse>> getVehicleModels(@RequestParam(required = false) UUID brandId) {
        return ResponseEntity.ok(vehicleCatalogService.listModels(brandId));
    }

    @PostMapping("/vehicle-models")
    @PreAuthorize("hasAuthority('catalog:write') or hasRole('ADMIN')")
    @Operation(summary = "Crear modelo de vehículo", description = "Crea un modelo de vehículo dentro de una marca")
    public ResponseEntity<VehicleModelResponse> createVehicleModel(@Valid @RequestBody VehicleModelRequest request) {
        return ResponseEntity.ok(vehicleCatalogService.createModel(request));
    }

    @PutMapping("/vehicle-models/{id}")
    @PreAuthorize("hasAuthority('catalog:write') or hasRole('ADMIN')")
    @Operation(summary = "Actualizar modelo de vehículo", description = "Actualiza un modelo de vehículo validando unicidad dentro de la marca")
    public ResponseEntity<VehicleModelResponse> updateVehicleModel(
            @PathVariable UUID id,
            @Valid @RequestBody VehicleModelRequest request
    ) {
        return ResponseEntity.ok(vehicleCatalogService.updateModel(id, request));
    }

    @GetMapping("/vehicle-versions")
    @PreAuthorize("hasAuthority('catalog:read') or hasRole('ADMIN')")
    @Operation(summary = "Listar versiones de vehículo", description = "Lista versiones de vehículo, filtrables por modelo")
    public ResponseEntity<List<VehicleVersionResponse>> getVehicleVersions(@RequestParam(required = false) UUID modelId) {
        return ResponseEntity.ok(vehicleCatalogService.listVersions(modelId));
    }

    @PostMapping("/vehicle-versions")
    @PreAuthorize("hasAuthority('catalog:write') or hasRole('ADMIN')")
    @Operation(summary = "Crear versión de vehículo", description = "Crea una versión de vehículo con rango de años opcional")
    public ResponseEntity<VehicleVersionResponse> createVehicleVersion(@Valid @RequestBody VehicleVersionRequest request) {
        return ResponseEntity.ok(vehicleCatalogService.createVersion(request));
    }

    @PutMapping("/vehicle-versions/{id}")
    @PreAuthorize("hasAuthority('catalog:write') or hasRole('ADMIN')")
    @Operation(summary = "Actualizar versión de vehículo", description = "Actualiza una versión de vehículo y su rango de años")
    public ResponseEntity<VehicleVersionResponse> updateVehicleVersion(
            @PathVariable UUID id,
            @Valid @RequestBody VehicleVersionRequest request
    ) {
        return ResponseEntity.ok(vehicleCatalogService.updateVersion(id, request));
    }

    @PutMapping("/variants/{id}/compatibility")
    @PreAuthorize("hasAuthority('catalog:write') or hasRole('ADMIN')")
    @Operation(summary = "Reemplazar compatibilidades de variante", description = "Reemplaza la lista de versiones de vehículo compatibles con la variante")
    public ResponseEntity<List<VehicleCompatibilityResponse>> replaceVariantCompatibility(
            @PathVariable UUID id,
            @Valid @RequestBody VehicleCompatibilityRequest request
    ) {
        return ResponseEntity.ok(vehicleCatalogService.replaceCompatibility(id, request));
    }

    @GetMapping("/variants/lookup")
    @PreAuthorize("hasAuthority('catalog:read') or hasRole('ADMIN')")
    @Operation(summary = "Buscar variante por código de barras", description = "Devuelve la variante y su producto a partir del código de barras; pensado para el escáner del POS")
    public ResponseEntity<VariantLookupResponse> lookupVariantByBarcode(@RequestParam String barcode) {
        return ResponseEntity.ok(vehicleCatalogService.lookupByBarcode(barcode));
    }
}
