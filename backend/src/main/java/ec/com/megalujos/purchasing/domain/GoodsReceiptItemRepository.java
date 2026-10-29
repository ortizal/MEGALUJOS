package ec.com.megalujos.purchasing.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GoodsReceiptItemRepository extends JpaRepository<GoodsReceiptItem, UUID> {

    @Query(value = """
            SELECT gri.order_item_id AS "orderItemId",
                   COALESCE(SUM(gri.quantity), 0) AS quantity
            FROM goods_receipt_items gri
            JOIN goods_receipts gr ON gr.id = gri.receipt_id
            WHERE gr.order_id = :orderId
            GROUP BY gri.order_item_id
            """, nativeQuery = true)
    List<ReceivedQuantityRow> sumReceivedByOrder(@Param("orderId") UUID orderId);
}
