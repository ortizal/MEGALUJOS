package ec.com.megalujos.identity.application;

import ec.com.megalujos.identity.domain.FailedLoginAttempt;
import ec.com.megalujos.identity.domain.FailedLoginAttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Registro de intentos de login fuera de la transacción principal:
 * un intento fallido debe persistir aunque la operación haga rollback.
 */
@Service
public class LoginAttemptService {

    private final FailedLoginAttemptRepository repository;

    public LoginAttemptService(FailedLoginAttemptRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String username, String ipAddress, boolean success) {
        repository.save(new FailedLoginAttempt(username, ipAddress, success));
    }

    /** Borra los fallos previos tras un login exitoso (se une a la transacción principal). */
    @Transactional
    public void clearFailures(String username) {
        repository.deleteByUsernameAndSuccessFalse(username);
    }

    @Transactional(readOnly = true)
    public long countRecentFailures(String username, OffsetDateTime since) {
        return repository.countByUsernameAndAttemptedAtAfterAndSuccessFalse(username, since);
    }
}
