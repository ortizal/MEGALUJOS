package ec.com.megalujos.services.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ServiceAdvanceRepository extends JpaRepository<ServiceAdvance, UUID> {

    List<ServiceAdvance> findByServiceOrderIdOrderByCreatedAtAsc(UUID serviceOrderId);
}
