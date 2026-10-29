package ec.com.megalujos.showcase.api;

import ec.com.megalujos.catalog.dto.ProductPublicDto;
import ec.com.megalujos.common.web.PageResponse;
import ec.com.megalujos.showcase.application.CatalogSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Catálogo público ampliado en la MISMA ruta que el catálogo del módulo
 * catálogo (/api/v1/public/catalog/products), que no se modifica. Para coexistir
 * sin ambigüedad de mappings se registran tres handlers con condiciones
 * {@code params} mutuamente exclusivas y número de expresiones distinto (1, 2 y 3),
 * de modo que Spring siempre elige un único handler:
 * - minPrice presente (sin sort)  → handler 1
 * - maxPrice presente sin minPrice (sin sort) → handler 2
 * - sort presente sin min/max → handler 3
 * Si no hay ningún filtro de precio ni sort, la petición la resuelve el
 * controlador existente del módulo catálogo (comportamiento previo intacto).
 */
@RestController
@RequestMapping("/api/v1/public/catalog")
@Tag(name = "Catálogo Público Ampliado", description = "Búsqueda pública de productos publicados con filtros de precio mínimo/máximo y orden")
public class PublicCatalogQueryController {

    private final CatalogSearchService catalogSearchService;

    public PublicCatalogQueryController(CatalogSearchService catalogSearchService) {
        this.catalogSearchService = catalogSearchService;
    }

    @GetMapping(value = "/products", params = "minPrice")
    @Operation(summary = "Buscar productos con precio mínimo", description = "Ampliación del catálogo público: aplica minPrice (y maxPrice/sort si vienen) sobre variantes activas de productos publicados")
    public ResponseEntity<PageResponse<ProductPublicDto>> searchWithMinPrice(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID brandId,
            @RequestParam(required = false) String search,
            @RequestParam BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return search(categoryId, brandId, search, minPrice, maxPrice, sort, page, size);
    }

    @GetMapping(value = "/products", params = {"maxPrice", "!minPrice"})
    @Operation(summary = "Buscar productos con precio máximo", description = "Ampliación del catálogo público: aplica maxPrice (y sort si viene) sobre variantes activas de productos publicados")
    public ResponseEntity<PageResponse<ProductPublicDto>> searchWithMaxPrice(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID brandId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return search(categoryId, brandId, search, null, maxPrice, sort, page, size);
    }

    @GetMapping(value = "/products", params = {"sort", "!minPrice", "!maxPrice"})
    @Operation(summary = "Buscar productos con orden", description = "Ampliación del catálogo público: aplica sort price-asc|price-desc|name-asc|name-desc")
    public ResponseEntity<PageResponse<ProductPublicDto>> searchWithSort(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID brandId,
            @RequestParam(required = false) String search,
            @RequestParam String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return search(categoryId, brandId, search, null, null, sort, page, size);
    }

    private ResponseEntity<PageResponse<ProductPublicDto>> search(UUID categoryId, UUID brandId, String search,
                                                                  BigDecimal minPrice, BigDecimal maxPrice,
                                                                  String sort, int page, int size) {
        Page<ProductPublicDto> result =
                catalogSearchService.searchProducts(categoryId, brandId, search, minPrice, maxPrice, sort, page, size);
        return ResponseEntity.ok(PageResponse.from(result));
    }
}
