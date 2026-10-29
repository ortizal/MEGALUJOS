package ec.com.megalujos.identity.api;

import ec.com.megalujos.common.security.UserPrincipal;
import ec.com.megalujos.common.web.PageResponse;
import ec.com.megalujos.identity.application.UserAdminService;
import ec.com.megalujos.identity.dto.CreateUserRequest;
import ec.com.megalujos.identity.dto.ResetPasswordRequest;
import ec.com.megalujos.identity.dto.UpdateUserActiveRequest;
import ec.com.megalujos.identity.dto.UpdateUserRequest;
import ec.com.megalujos.identity.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Administración de Usuarios", description = "Gestión de usuarios del sistema: creación, edición, estados y contraseñas")
@PreAuthorize("hasAuthority('users:manage') or hasRole('ADMIN')")
public class UserAdminController {

    private final UserAdminService userAdminService;

    public UserAdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    @Operation(summary = "Listar usuarios", description = "Pagina los usuarios con filtro de búsqueda (q) y estado activo/inactivo")
    public ResponseEntity<PageResponse<UserResponse>> list(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "active", required = false) Boolean active,
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<UserResponse> response = userAdminService.list(q, active, pageable);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Crear usuario", description = "Crea un usuario con contraseña cifrada y roles asignados")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request,
                                               @AuthenticationPrincipal UserPrincipal principal,
                                               HttpServletRequest httpRequest) {
        UserResponse response = userAdminService.create(request, principal.getId(), httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar usuario", description = "Actualiza datos personales y roles de un usuario existente")
    public ResponseEntity<UserResponse> update(@PathVariable UUID id,
                                               @Valid @RequestBody UpdateUserRequest request,
                                               @AuthenticationPrincipal UserPrincipal principal,
                                               HttpServletRequest httpRequest) {
        UserResponse response = userAdminService.update(id, request, principal.getId(), httpRequest.getRemoteAddr());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/active")
    @Operation(summary = "Cambiar estado", description = "Activa o desactiva un usuario")
    public ResponseEntity<UserResponse> changeActive(@PathVariable UUID id,
                                                     @Valid @RequestBody UpdateUserActiveRequest request,
                                                     @AuthenticationPrincipal UserPrincipal principal,
                                                     HttpServletRequest httpRequest) {
        UserResponse response = userAdminService.changeActive(id, request, principal.getId(), httpRequest.getRemoteAddr());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "Restablecer contraseña", description = "Reemplaza la contraseña de un usuario sin validar la anterior")
    public ResponseEntity<Void> resetPassword(@PathVariable UUID id,
                                              @Valid @RequestBody ResetPasswordRequest request,
                                              @AuthenticationPrincipal UserPrincipal principal,
                                              HttpServletRequest httpRequest) {
        userAdminService.resetPassword(id, request, principal.getId(), httpRequest.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }
}
