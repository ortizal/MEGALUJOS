package ec.com.megalujos.invoicing.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IssuerRepository extends JpaRepository<Issuer, UUID> {
    List<Issuer> findAllByOrderByCreatedAtAsc();
    Optional<Issuer> findFirstByActiveTrue();
    boolean existsByActiveTrue();
}
