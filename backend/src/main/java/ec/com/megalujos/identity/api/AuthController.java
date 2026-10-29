package ec.com.megalujos.identity.api;

import ec.com.megalujos.common.security.UserPrincipal;
import ec.com.megalujos.identity.application.AuthService;
import ec.com.megalujos.identity.dto.ChangePasswordRequest;
import ec.com.megalujos.identity.dto.LoginRequest;
import ec.com.megalujos.identity.dto.LoginResponse;
import ec.com.megalujos.identity.dto.RefreshTokenRequest;
import ec.com.megalujos.identity.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Seguridad y Autenticación", description = "Endpoints para inicio de sesión y gestión de credenciales")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Autentica al usuario con username y password, devolviendo JWT token y permisos")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        LoginResponse response = authService.login(request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refrescar token", description = "Rota el refresh token y emite nuevos access + refresh tokens")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = authService.refresh(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cambiar contraseña", description = "Cambia la contraseña del usuario autenticado validando la contraseña actual")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal UserPrincipal principal,
                                               @Valid @RequestBody ChangePasswordRequest request,
                                               HttpServletRequest httpRequest) {
        authService.changePassword(principal.getId(), request, httpRequest.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Usuario actual", description = "Devuelve perfil, roles y permisos del usuario autenticado")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        UserResponse response = authService.getMe(principal.getId());
        return ResponseEntity.ok(response);
    }
}
