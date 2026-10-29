package ec.com.megalujos.pos.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface SaleReturnRepository extends JpaRepository<SaleReturn, UUID> {

    List<SaleReturn> findBySaleIdOrderByCreatedAtDesc(UUID saleId);

    @Query("SELECT COALESCE(SUM(r.totalReturned), 0) FROM SaleReturn r WHERE r.saleId = :saleId")
    BigDecimal sumTotalReturnedBySaleId(@Param("saleId") UUID saleId);

    @Query("SELECT r.saleId, COALESCE(SUM(r.totalReturned), 0) FROM SaleReturn r WHERE r.saleId IN :saleIds GROUP BY r.saleId")
    List<Object[]> sumTotalReturnedBySaleIds(@Param("saleIds") List<UUID> saleIds);
}
