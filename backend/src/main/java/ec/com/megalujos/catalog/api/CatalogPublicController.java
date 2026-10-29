package ec.com.megalujos.catalog.api;

import ec.com.megalujos.catalog.application.BrandService;
import ec.com.megalujos.catalog.application.CategoryService;
import ec.com.megalujos.catalog.application.ProductService;
import ec.com.megalujos.catalog.domain.Brand;
import ec.com.megalujos.catalog.domain.Category;
import ec.com.megalujos.catalog.dto.ProductPublicDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/catalog")
@Tag(name = "Catálogo Público y Vitrina", description = "Endpoints públicos para visualización de catálogo, búsqueda y precios de Megalujos")
public class CatalogPublicController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final BrandService brandService;

    public CatalogPublicController(ProductService productService, CategoryService categoryService, BrandService brandService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.brandService = brandService;
    }

    @GetMapping("/products")
    @Operation(summary = "Listar productos públicos", description = "Retorna productos publicados con paginación, filtros de categoría, marca y búsqueda por texto")
    public ResponseEntity<Page<ProductPublicDto>> getPublishedProducts(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID brandId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "name") String sort
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort).ascending());
        Page<ProductPublicDto> result = productService.getPublishedProducts(categoryId, brandId, search, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/products/{id}")
    @Operation(summary = "Detalle público de producto", description = "Obtiene los detalles, fotos, especificaciones y botón WhatsApp para un producto")
    public ResponseEntity<ProductPublicDto> getProductDetail(@PathVariable UUID id) {
        ProductPublicDto dto = productService.getProductPublicDetail(id);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/categories")
    @Operation(summary = "Listar categorías activas", description = "Lista todas las categorías automotrices y de servicios activas")
    public ResponseEntity<List<Category>> getCategories() {
        return ResponseEntity.ok(categoryService.getActiveCategories());
    }

    @GetMapping("/brands")
    @Operation(summary = "Listar marcas automotrices", description = "Lista todas las marcas registradas y activas")
    public ResponseEntity<List<Brand>> getBrands() {
        return ResponseEntity.ok(brandService.getActiveBrands());
    }
}
