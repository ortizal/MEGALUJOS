package ec.com.megalujos.identity.application;

import ec.com.megalujos.identity.domain.FailedLoginAttempt;
import ec.com.megalujos.identity.domain.FailedLoginAttemptRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios de LoginAttemptService: registro y limpieza de intentos fallidos.
 * El bloqueo de cuenta (ACCOUNT_LOCKED) se valida en AuthServiceTest.
 */
@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

    @Mock
    private FailedLoginAttemptRepository repository;

    @InjectMocks
    private LoginAttemptService loginAttemptService;

    @Test
    void record_persisteElIntentoFallidoConLosCamposEsperados() {
        // Given
        when(repository.save(any(FailedLoginAttempt.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        loginAttemptService.record("juan", "10.0.0.1", false);

        // Then
        ArgumentCaptor<FailedLoginAttempt> captor = ArgumentCaptor.forClass(FailedLoginAttempt.class);
        verify(repository).save(captor.capture());
        FailedLoginAttempt saved = captor.getValue();
        assertEquals("juan", saved.getUsername());
        assertEquals("10.0.0.1", saved.getIpAddress());
        assertFalse(saved.isSuccess());
        assertNotNull(saved.getAttemptedAt());
    }

    @Test
    void record_marcaElIntentoExitoso() {
        // Given
        when(repository.save(any(FailedLoginAttempt.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        loginAttemptService.record("juan", "10.0.0.1", true);

        // Then
        ArgumentCaptor<FailedLoginAttempt> captor = ArgumentCaptor.forClass(FailedLoginAttempt.class);
        verify(repository).save(captor.capture());
        assertTrue(captor.getValue().isSuccess());
    }

    @Test
    void clearFailures_borraLosFallosPreviosDelUsuario() {
        // When
        loginAttemptService.clearFailures("juan");

        // Then
        verify(repository).deleteByUsernameAndSuccessFalse("juan");
    }

    @Test
    void countRecentFailures_devuelveElConteoDelRepositorio() {
        // Given
        OffsetDateTime since = OffsetDateTime.now().minusMinutes(15);
        when(repository.countByUsernameAndAttemptedAtAfterAndSuccessFalse("juan", since)).thenReturn(5L);

        // When
        long failures = loginAttemptService.countRecentFailures("juan", since);

        // Then
        assertEquals(5L, failures);
    }
}
