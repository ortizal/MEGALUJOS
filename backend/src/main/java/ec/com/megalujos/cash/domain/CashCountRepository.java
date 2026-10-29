package ec.com.megalujos.cash.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CashCountRepository extends JpaRepository<CashCount, UUID> {

    Optional<CashCount> findBySessionIdAndMethod(UUID sessionId, String method);

    List<CashCount> findBySessionIdOrderByMethodAsc(UUID sessionId);
}
