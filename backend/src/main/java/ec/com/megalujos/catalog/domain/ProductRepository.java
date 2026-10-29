package ec.com.megalujos.catalog.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.published = true " +
           "AND (CAST(:categoryId AS STRING) IS NULL OR p.category.id = :categoryId) " +
           "AND (CAST(:brandId AS STRING) IS NULL OR p.brand.id = :brandId) " +
           "AND (CAST(:searchPattern AS STRING) IS NULL OR p.name ILIKE CAST(:searchPattern AS STRING) OR p.sku ILIKE CAST(:searchPattern AS STRING))")
    Page<Product> findPublishedProducts(
            @Param("categoryId") UUID categoryId,
            @Param("brandId") UUID brandId,
            @Param("searchPattern") String searchPattern,
            Pageable pageable
    );

    @Query("SELECT p FROM Product p WHERE " +
           "(CAST(:categoryId AS STRING) IS NULL OR p.category.id = :categoryId) " +
           "AND (CAST(:searchPattern AS STRING) IS NULL OR p.name ILIKE CAST(:searchPattern AS STRING) OR p.sku ILIKE CAST(:searchPattern AS STRING))")
    Page<Product> findAllProductsAdmin(
            @Param("categoryId") UUID categoryId,
            @Param("searchPattern") String searchPattern,
            Pageable pageable
    );

    @Query("SELECT p FROM Product p WHERE " +
           "(CAST(:q AS STRING) IS NULL OR p.name ILIKE CONCAT('%', CAST(:q AS STRING), '%') OR p.sku ILIKE CONCAT('%', CAST(:q AS STRING), '%')) " +
           "AND (CAST(:categoryId AS STRING) IS NULL OR p.category.id = :categoryId) " +
           "AND (CAST(:brandId AS STRING) IS NULL OR p.brand.id = :brandId) " +
           "AND (CAST(:published AS STRING) IS NULL OR p.published = :published) " +
           "AND (CAST(:active AS STRING) IS NULL OR p.active = :active) " +
           "AND (CAST(:productType AS STRING) IS NULL OR UPPER(p.productType) = :productType)")
    Page<Product> findAllAdmin(
            @Param("q") String q,
            @Param("categoryId") UUID categoryId,
            @Param("brandId") UUID brandId,
            @Param("published") Boolean published,
            @Param("active") Boolean active,
            @Param("productType") String productType,
            Pageable pageable
    );
}
