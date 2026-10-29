package ec.com.megalujos.purchasing.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID> {

    @Query("SELECT po FROM PurchaseOrder po WHERE " +
           "(CAST(:status AS STRING) IS NULL OR po.status = :status) AND " +
           "(CAST(:q AS STRING) IS NULL OR po.orderNumber ILIKE CONCAT('%', CAST(:q AS STRING), '%') OR EXISTS " +
           "(SELECT 1 FROM Supplier s WHERE s.id = po.supplierId AND s.name ILIKE CONCAT('%', CAST(:q AS STRING), '%')))")
    Page<PurchaseOrder> search(
            @Param("status") String status,
            @Param("q") String q,
            Pageable pageable
    );
}
