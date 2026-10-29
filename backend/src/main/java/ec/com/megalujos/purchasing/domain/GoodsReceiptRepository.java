package ec.com.megalujos.purchasing.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, UUID> {
    Page<GoodsReceipt> findByOrderIdOrderByReceivedAtDesc(UUID orderId, Pageable pageable);
}
