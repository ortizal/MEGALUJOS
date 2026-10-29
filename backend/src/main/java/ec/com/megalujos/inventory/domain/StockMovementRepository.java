package ec.com.megalujos.inventory.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {
    Page<StockMovement> findByVariantIdOrderByCreatedAtDesc(UUID variantId, Pageable pageable);
    List<StockMovement> findBySourceTypeAndSourceId(String sourceType, UUID sourceId);

    @Query("SELECT m FROM StockMovement m WHERE " +
           "(CAST(:variantId AS STRING) IS NULL OR m.variantId = :variantId) AND " +
           "(CAST(:warehouseId AS STRING) IS NULL OR m.warehouseId = :warehouseId) AND " +
           "(CAST(:movementType AS STRING) IS NULL OR m.movementType = :movementType) AND " +
           "(CAST(:dateFrom AS STRING) IS NULL OR m.createdAt >= :dateFrom) AND " +
           "(CAST(:dateTo AS STRING) IS NULL OR m.createdAt <= :dateTo)")
    Page<StockMovement> searchKardex(
            @Param("variantId") UUID variantId,
            @Param("warehouseId") UUID warehouseId,
            @Param("movementType") String movementType,
            @Param("dateFrom") OffsetDateTime dateFrom,
            @Param("dateTo") OffsetDateTime dateTo,
            Pageable pageable
    );
}
