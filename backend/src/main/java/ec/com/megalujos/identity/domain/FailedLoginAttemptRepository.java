package ec.com.megalujos.identity.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
public interface FailedLoginAttemptRepository extends JpaRepository<FailedLoginAttempt, UUID> {
    long countByUsernameAndAttemptedAtAfterAndSuccessFalse(String username, OffsetDateTime attemptedAtAfter);
    void deleteByUsernameAndSuccessFalse(String username);
}
