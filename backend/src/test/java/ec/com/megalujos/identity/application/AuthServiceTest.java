package ec.com.megalujos.identity.application;

import ec.com.megalujos.common.audit.AuditService;
import ec.com.megalujos.common.exception.BusinessRuleException;
import ec.com.megalujos.identity.domain.RefreshTokenRepository;
import ec.com.megalujos.identity.domain.User;
import ec.com.megalujos.identity.domain.UserRepository;
import ec.com.megalujos.identity.dto.LoginRequest;
import ec.com.megalujos.identity.dto.LoginResponse;
import ec.com.megalujos.common.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios del bloqueo por intentos fallidos de login en AuthService.
 * Config: maxLoginAttempts=5, lockoutMinutes=15.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtTokenProvider tokenProvider;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private LoginAttemptService loginAttemptService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuditService auditService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(authenticationManager, userRepository, tokenProvider,
                refreshTokenRepository, loginAttemptService, passwordEncoder, auditService,
                5, 15, 7);
    }

    @Test
    void login_conCincoFallosRecientes_lanzaAccountLockedConStatus423() {
        // Given: 5 fallos dentro de la ventana de bloqueo
        when(loginAttemptService.countRecentFailures(eq("juan"), any(OffsetDateTime.class))).thenReturn(5L);

        // When / Then
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> authService.login(new LoginRequest("juan", "mala"), "10.0.0.1"));

        assertEquals("ACCOUNT_LOCKED", ex.getCode());
        assertEquals(HttpStatus.LOCKED, ex.getStatus());
        assertEquals(423, ex.getStatus().value());
        // Nunca llega a intentar autenticar
        verifyNoInteractions(authenticationManager);
    }

    @Test void login_conCuatroFallosNoBloqueaYElFalloSeRegistra() {
        // Given: solo 4 fallos (< 5) => no bloquea
        when(loginAttemptService.countRecentFailures(eq("juan"), any(OffsetDateTime.class))).thenReturn(4L);
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Credenciales inválidas"));

        // When / Then: el fallo de autenticación se propaga y se registra
        assertThrows(BadCredentialsException.class,
                () -> authService.login(new LoginRequest("juan", "mala"), "10.0.0.1"));
        verify(loginAttemptService).record("juan", "10.0.0.1", false);
    }

    @Test
    void login_consultaFalrosDentroDeLaVentanaDeLockout() {
        // Given
        when(loginAttemptService.countRecentFailures(eq("juan"), any(OffsetDateTime.class))).thenReturn(0L);
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Credenciales inválidas"));
        OffsetDateTime antes = OffsetDateTime.now();

        // When
        assertThrows(BadCredentialsException.class,
                () -> authService.login(new LoginRequest("juan", "mala"), "10.0.0.1"));

        // Then: el "since" debe ser ~15 minutos atrás (ventana de lockout)
        ArgumentCaptor<OffsetDateTime> captor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(loginAttemptService).countRecentFailures(eq("juan"), captor.capture());
        OffsetDateTime since = captor.getValue();
        OffsetDateTime esperadoMin = antes.minusMinutes(16);
        OffsetDateTime esperadoMax = OffsetDateTime.now().minusMinutes(14);
        assertTrue(since.isAfter(esperadoMin) && since.isBefore(esperadoMax),
                "La ventana de lockout debe ser ~15 minutos, fue: " + since);
    }

    @Test
    void loginExitoso_limpiaLosFallosPreviosYRegistraElIntentoExitoso() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("juan");
        user.setFullName("Juan Pérez");
        user.setActive(true);

        when(loginAttemptService.countRecentFailures(eq("juan"), any(OffsetDateTime.class))).thenReturn(0L);
        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(userRepository.findByUsername("juan")).thenReturn(java.util.Optional.of(user));
        when(tokenProvider.generateAccessToken(any(), any(), anyList(), anyList())).thenReturn("access-token");
        when(tokenProvider.generateRefreshToken(any(), any())).thenReturn("refresh-token");

        // When
        LoginResponse response = authService.login(new LoginRequest("juan", "buena"), "10.0.0.1");

        // Then
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("juan", response.getUsername());
        verify(loginAttemptService).record("juan", "10.0.0.1", true);
        verify(loginAttemptService).clearFailures("juan");
        assertNotNull(user.getLastLoginAt());
        verify(userRepository).save(user);
        verify(refreshTokenRepository).save(any());
    }

    @Test
    void login_usuarioInactivo_registraFalloYRechaza() {
        // Given
        User user = new User();
        user.setUsername("juan");
        user.setActive(false);
        when(loginAttemptService.countRecentFailures(eq("juan"), any(OffsetDateTime.class))).thenReturn(0L);
        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(userRepository.findByUsername("juan")).thenReturn(java.util.Optional.of(user));

        // When / Then
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> authService.login(new LoginRequest("juan", "buena"), "10.0.0.1"));
        assertEquals("USER_INACTIVE", ex.getCode());
        verify(loginAttemptService).record("juan", "10.0.0.1", false);
        verify(loginAttemptService, never()).clearFailures(any());
    }
}
