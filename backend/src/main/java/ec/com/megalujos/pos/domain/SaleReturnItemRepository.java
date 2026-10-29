package ec.com.megalujos.pos.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
public interface SaleReturnItemRepository extends JpaRepository<SaleReturnItem, UUID> {

    @Query("SELECT COALESCE(SUM(ri.quantity), 0) FROM SaleReturnItem ri WHERE ri.saleItemId = :saleItemId")
    BigDecimal sumReturnedQuantityBySaleItem(@Param("saleItemId") UUID saleItemId);
}
