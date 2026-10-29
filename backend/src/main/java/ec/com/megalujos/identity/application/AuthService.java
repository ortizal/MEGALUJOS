package ec.com.megalujos.identity.application;

import ec.com.megalujos.common.audit.AuditService;
import ec.com.megalujos.common.exception.BusinessRuleException;
import ec.com.megalujos.common.exception.ResourceNotFoundException;
import ec.com.megalujos.common.security.JwtTokenProvider;
import ec.com.megalujos.identity.domain.Permission;
import ec.com.megalujos.identity.domain.RefreshToken;
import ec.com.megalujos.identity.domain.RefreshTokenRepository;
import ec.com.megalujos.identity.domain.Role;
import ec.com.megalujos.identity.domain.User;
import ec.com.megalujos.identity.domain.UserRepository;
import ec.com.megalujos.identity.dto.ChangePasswordRequest;
import ec.com.megalujos.identity.dto.LoginRequest;
import ec.com.megalujos.identity.dto.LoginResponse;
import ec.com.megalujos.identity.dto.RefreshTokenRequest;
import ec.com.megalujos.identity.dto.UserResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginAttemptService loginAttemptService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final int maxLoginAttempts;
    private final long lockoutMinutes;
    private final long refreshExpirationDays;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       JwtTokenProvider tokenProvider,
                       RefreshTokenRepository refreshTokenRepository,
                       LoginAttemptService loginAttemptService,
                       PasswordEncoder passwordEncoder,
                       AuditService auditService,
                       @Value("${megalujos.security.login.max-attempts:5}") int maxLoginAttempts,
                       @Value("${megalujos.security.login.lockout-minutes:15}") long lockoutMinutes,
                       @Value("${megalujos.security.jwt.refresh-expiration-days:7}") long refreshExpirationDays) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.loginAttemptService = loginAttemptService;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.maxLoginAttempts = maxLoginAttempts;
        this.lockoutMinutes = lockoutMinutes;
        this.refreshExpirationDays = refreshExpirationDays;
    }

    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress) {
        assertNotLocked(request.getUsername());

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (AuthenticationException ex) {
            loginAttemptService.record(request.getUsername(), ipAddress, false);
            throw ex;
        }

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessRuleException("INVALID_USER", "Usuario no encontrado"));

        if (!user.isActive()) {
            loginAttemptService.record(request.getUsername(), ipAddress, false);
            throw new BusinessRuleException("USER_INACTIVE", "El usuario se encuentra inactivo");
        }

        loginAttemptService.record(request.getUsername(), ipAddress, true);
        loginAttemptService.clearFailures(request.getUsername());

        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);

        List<String> roles = roleCodes(user);
        List<String> permissions = permissionCodes(user);

        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getUsername(), roles, permissions);
        String refreshToken = tokenProvider.generateRefreshToken(user.getId(), user.getUsername());
        storeRefreshToken(user.getId(), refreshToken);

        return new LoginResponse(
                accessToken,
                refreshToken,
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                roles,
                permissions
        );
    }

    /** Rota el refresh token: revoca el actual y emite nuevos access + refresh. */
    @Transactional
    public LoginResponse refresh(RefreshTokenRequest request) {
        String tokenHash = sha256Hex(request.getRefreshToken());
        RefreshToken current = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessRuleException("INVALID_REFRESH_TOKEN", "Token de refresco inválido", HttpStatus.UNAUTHORIZED));

        if (current.isRevoked()) {
            throw new BusinessRuleException("REFRESH_TOKEN_REVOKED", "El token de refresco fue revocado", HttpStatus.UNAUTHORIZED);
        }
        if (current.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new BusinessRuleException("REFRESH_TOKEN_EXPIRED", "El token de refresco ha expirado", HttpStatus.UNAUTHORIZED);
        }

        User user = userRepository.findById(current.getUserId())
                .orElseThrow(() -> new BusinessRuleException("INVALID_USER", "Usuario no encontrado", HttpStatus.UNAUTHORIZED));

        if (!user.isActive()) {
            throw new BusinessRuleException("USER_INACTIVE", "El usuario se encuentra inactivo", HttpStatus.UNAUTHORIZED);
        }

        List<String> roles = roleCodes(user);
        List<String> permissions = permissionCodes(user);

        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getUsername(), roles, permissions);
        String newRefreshToken = tokenProvider.generateRefreshToken(user.getId(), user.getUsername());
        RefreshToken stored = storeRefreshToken(user.getId(), newRefreshToken);

        current.setRevoked(true);
        current.setReplacedBy(stored.getId());
        refreshTokenRepository.save(current);

        return new LoginResponse(
                accessToken,
                newRefreshToken,
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                roles,
                permissions
        );
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException("INVALID_PASSWORD", "La contraseña actual es incorrecta");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        auditService.record(userId, "PASSWORD_CHANGED", "USER", userId,
                Map.of("username", user.getUsername()), ipAddress);
    }

    @Transactional(readOnly = true)
    public UserResponse getMe(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", userId));

        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setRoles(roleCodes(user));
        response.setPermissions(permissionCodes(user));
        return response;
    }

    private void assertNotLocked(String username) {
        OffsetDateTime since = OffsetDateTime.now().minusMinutes(lockoutMinutes);
        long failedAttempts = loginAttemptService.countRecentFailures(username, since);
        if (failedAttempts >= maxLoginAttempts) {
            throw new BusinessRuleException(
                    "ACCOUNT_LOCKED",
                    "Cuenta bloqueada por intentos fallidos. Intente de nuevo más tarde",
                    HttpStatus.LOCKED
            );
        }
    }

    private RefreshToken storeRefreshToken(UUID userId, String refreshToken) {
        RefreshToken entity = new RefreshToken();
        entity.setUserId(userId);
        entity.setTokenHash(sha256Hex(refreshToken));
        entity.setExpiresAt(OffsetDateTime.now().plusDays(refreshExpirationDays));
        entity.setRevoked(false);
        return refreshTokenRepository.save(entity);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Algoritmo SHA-256 no disponible", ex);
        }
    }

    private List<String> roleCodes(User user) {
        List<String> roles = new ArrayList<>();
        for (Role role : user.getRoles()) {
            roles.add(role.getCode());
        }
        return roles;
    }

    private List<String> permissionCodes(User user) {
        List<String> permissions = new ArrayList<>();
        for (Role role : user.getRoles()) {
            for (Permission perm : role.getPermissions()) {
                if (!permissions.contains(perm.getCode())) {
                    permissions.add(perm.getCode());
                }
            }
        }
        return permissions;
    }
}
