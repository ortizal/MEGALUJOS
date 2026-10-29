package ec.com.megalujos.cash.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CashSessionRepository extends JpaRepository<CashSession, UUID> {
    Optional<CashSession> findByRegisterIdAndStatus(UUID registerId, String status);
    Optional<CashSession> findFirstByStatusOrderByOpenedAtDesc(String status);
    List<CashSession> findByStatusOrderByOpenedAtDesc(String status);
}
