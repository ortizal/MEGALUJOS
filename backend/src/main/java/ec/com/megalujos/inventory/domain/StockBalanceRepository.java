package ec.com.megalujos.inventory.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockBalanceRepository extends JpaRepository<StockBalance, StockBalanceId> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT sb FROM StockBalance sb WHERE sb.id = :id")
    Optional<StockBalance> findByIdForUpdate(@Param("id") StockBalanceId id);

    @Query("SELECT sb FROM StockBalance sb WHERE sb.id.variantId = :variantId")
    List<StockBalance> findByVariantId(@Param("variantId") UUID variantId);

    @Query("SELECT sb FROM StockBalance sb WHERE sb.id.warehouseId = :warehouseId")
    List<StockBalance> findByWarehouseId(@Param("warehouseId") UUID warehouseId);

    @Query(value = """
            SELECT sb.variant_id AS "variantId", sb.warehouse_id AS "warehouseId",
                   sb.quantity AS quantity, sb.reserved AS reserved,
                   pv.min_stock AS "minStock", pv.sku AS sku,
                   p.name AS "productName", w.name AS "warehouseName"
            FROM stock_balances sb
            JOIN product_variants pv ON pv.id = sb.variant_id
            JOIN products p ON p.id = pv.product_id
            JOIN warehouses w ON w.id = sb.warehouse_id
            WHERE (CAST(:q AS TEXT) IS NULL OR p.name ILIKE CAST(:q AS TEXT) OR pv.sku ILIKE CAST(:q AS TEXT))
              AND (CAST(:warehouseId AS uuid) IS NULL OR sb.warehouse_id = :warehouseId)
              AND (CAST(:lowStockOnly AS boolean) = false OR (sb.quantity - sb.reserved) <= pv.min_stock)
            """, countQuery = """
            SELECT COUNT(*)
            FROM stock_balances sb
            JOIN product_variants pv ON pv.id = sb.variant_id
            JOIN products p ON p.id = pv.product_id
            WHERE (CAST(:q AS TEXT) IS NULL OR p.name ILIKE CAST(:q AS TEXT) OR pv.sku ILIKE CAST(:q AS TEXT))
              AND (CAST(:warehouseId AS uuid) IS NULL OR sb.warehouse_id = :warehouseId)
              AND (CAST(:lowStockOnly AS boolean) = false OR (sb.quantity - sb.reserved) <= pv.min_stock)
            """, nativeQuery = true)
    Page<StockBalanceRow> searchBalances(
            @Param("q") String q,
            @Param("warehouseId") UUID warehouseId,
            @Param("lowStockOnly") boolean lowStockOnly,
            Pageable pageable
    );

    @Query(value = """
            SELECT sb.variant_id AS "variantId",
                   (sb.quantity - sb.reserved) AS quantity,
                   pv.min_stock AS "minStock", pv.sku AS sku,
                   p.name AS "productName"
            FROM stock_balances sb
            JOIN product_variants pv ON pv.id = sb.variant_id
            JOIN products p ON p.id = pv.product_id
            WHERE (sb.quantity - sb.reserved) <= pv.min_stock
            ORDER BY p.name
            """, nativeQuery = true)
    List<StockAlertRow> findLowStockAlerts();
}
