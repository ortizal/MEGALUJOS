package ec.com.megalujos.showcase.application;

import ec.com.megalujos.catalog.application.ProductService;
import ec.com.megalujos.catalog.domain.Product;
import ec.com.megalujos.catalog.dto.ProductPublicDto;
import ec.com.megalujos.showcase.domain.ProductPublicQueryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Búsqueda pública de catálogo con filtros de precio mínimo/máximo y orden
 * price-asc|price-desc|name-asc|name-desc. Usa queries de solo lectura del
 * propio módulo (ProductPublicQueryRepository) y el mapeo público de
 * ProductService sin modificar el módulo catálogo.
 */
@Service
public class CatalogSearchService {

    private final ProductPublicQueryRepository productPublicQueryRepository;
    private final ProductService productService;

    public CatalogSearchService(ProductPublicQueryRepository productPublicQueryRepository,
                                ProductService productService) {
        this.productPublicQueryRepository = productPublicQueryRepository;
        this.productService = productService;
    }

    @Transactional(readOnly = true)
    public Page<ProductPublicDto> searchProducts(UUID categoryId, UUID brandId, String search,
                                                 BigDecimal minPrice, BigDecimal maxPrice,
                                                 String sort, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        long total = productPublicQueryRepository.countPublished(categoryId, brandId, search, minPrice, maxPrice);

        List<ProductPublicQueryRepository.ProductPriceRow> rows = productPublicQueryRepository
                .searchPublished(categoryId, brandId, search, minPrice, maxPrice, sort,
                        (int) pageable.getOffset(), pageable.getPageSize());

        List<UUID> ids = rows.stream().map(ProductPublicQueryRepository.ProductPriceRow::productId).toList();
        Map<UUID, Product> loaded = new HashMap<>();
        for (Product product : productPublicQueryRepository.findAllByIds(ids)) {
            loaded.put(product.getId(), product);
        }

        List<ProductPublicDto> ordered = new ArrayList<>();
        for (UUID id : ids) {
            Product product = loaded.get(id);
            if (product != null) {
                ordered.add(productService.mapToPublicDto(product));
            }
        }
        return new PageImpl<>(ordered, pageable, total);
    }
}
