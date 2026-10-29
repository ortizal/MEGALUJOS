package ec.com.megalujos.inventory.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface StockCountRepository extends JpaRepository<StockCount, UUID> {
    Page<StockCount> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
