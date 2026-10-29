package ec.com.megalujos.catalog.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {

    boolean existsByProductIdAndMediaFileId(UUID productId, UUID mediaId);

    /** Desmarca la imagen primaria del producto (índice parcial UNIQUE exige un solo is_primary). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE ProductImage pi SET pi.isPrimary = false WHERE pi.product.id = :productId AND pi.isPrimary = true")
    int clearPrimary(@Param("productId") UUID productId);
}
