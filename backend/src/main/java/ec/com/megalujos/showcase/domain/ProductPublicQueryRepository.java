package ec.com.megalujos.showcase.domain;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Consultas de SOLO LECTURA sobre productos publicados del catálogo para la
 * búsqueda pública de la vitrina (filtros de precio y orden). Vive en el paquete
 * showcase para no modificar el módulo catálogo; no escribe datos.
 */
@Repository
public class ProductPublicQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /** Fila de agregación: producto y rango de precios de sus variantes activas. */
    public record ProductPriceRow(UUID productId, BigDecimal minPrice, BigDecimal maxPrice) {
    }

    public long countPublished(UUID categoryId, UUID brandId, String search,
                               BigDecimal minPrice, BigDecimal maxPrice) {
        Map<String, Object> params = new HashMap<>();
        String where = buildWhere(categoryId, brandId, search, minPrice, maxPrice, params);
        TypedQuery<Long> countQuery = entityManager.createQuery(
                "SELECT COUNT(DISTINCT p.id) FROM Product p JOIN p.variants v" + where, Long.class);
        applyParams(countQuery, params);
        return countQuery.getSingleResult();
    }

    public List<ProductPriceRow> searchPublished(UUID categoryId, UUID brandId, String search,
                                                 BigDecimal minPrice, BigDecimal maxPrice,
                                                 String sort, int offset, int limit) {
        Map<String, Object> params = new HashMap<>();
        String where = buildWhere(categoryId, brandId, search, minPrice, maxPrice, params);
        String orderClause = orderClause(sort);
        TypedQuery<Object[]> query = entityManager.createQuery(
                "SELECT p.id, MIN(v.salePrice), MAX(v.salePrice) FROM Product p JOIN p.variants v"
                        + where + " GROUP BY p.id, p.name" + orderClause, Object[].class);
        applyParams(query, params);
        List<Object[]> rows = query
                .setFirstResult(Math.max(0, offset))
                .setMaxResults(Math.max(1, limit))
                .getResultList();
        return rows.stream()
                .map(row -> new ProductPriceRow((UUID) row[0], (BigDecimal) row[1], (BigDecimal) row[2]))
                .toList();
    }

    public List<ec.com.megalujos.catalog.domain.Product> findAllByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return entityManager.createQuery(
                        "SELECT p FROM Product p WHERE p.id IN :ids", ec.com.megalujos.catalog.domain.Product.class)
                .setParameter("ids", ids)
                .getResultList();
    }

    private String buildWhere(UUID categoryId, UUID brandId, String search,
                              BigDecimal minPrice, BigDecimal maxPrice, Map<String, Object> params) {
        StringBuilder where = new StringBuilder(
                " WHERE p.active = true AND p.published = true AND v.active = true ");
        if (categoryId != null) {
            where.append("AND p.category.id = :categoryId ");
            params.put("categoryId", categoryId);
        }
        if (brandId != null) {
            where.append("AND p.brand.id = :brandId ");
            params.put("brandId", brandId);
        }
        if (search != null && !search.isBlank()) {
            where.append("AND (p.name ILIKE CAST(:search AS TEXT) OR p.sku ILIKE CAST(:search AS TEXT)) ");
            params.put("search", "%" + search + "%");
        }
        if (minPrice != null) {
            where.append("AND v.salePrice >= :minPrice ");
            params.put("minPrice", minPrice);
        }
        if (maxPrice != null) {
            where.append("AND v.salePrice <= :maxPrice ");
            params.put("maxPrice", maxPrice);
        }
        return where.toString();
    }

    private String orderClause(String sort) {
        String normalized = sort == null ? "name" : sort.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "price-asc" -> " ORDER BY MIN(v.salePrice) ASC, p.name ASC";
            case "price-desc" -> " ORDER BY MIN(v.salePrice) DESC, p.name ASC";
            case "name-desc" -> " ORDER BY p.name DESC";
            default -> " ORDER BY p.name ASC";
        };
    }

    private void applyParams(TypedQuery<?> query, Map<String, Object> params) {
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
    }
}
