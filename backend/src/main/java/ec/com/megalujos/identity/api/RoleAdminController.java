package ec.com.megalujos.identity.api;

import ec.com.megalujos.identity.application.RoleAdminService;
import ec.com.megalujos.identity.dto.PermissionResponse;
import ec.com.megalujos.identity.dto.RoleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@Tag(name = "Roles y Permisos", description = "Consulta de roles y permisos del sistema")
@PreAuthorize("hasAuthority('users:manage') or hasRole('ADMIN')")
public class RoleAdminController {

    private final RoleAdminService roleAdminService;

    public RoleAdminController(RoleAdminService roleAdminService) {
        this.roleAdminService = roleAdminService;
    }

    @GetMapping
    @Operation(summary = "Listar roles", description = "Devuelve los roles con sus permisos asignados")
    public ResponseEntity<List<RoleResponse>> list() {
        List<RoleResponse> response = roleAdminService.listRoles();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/permissions")
    @Operation(summary = "Listar permisos", description = "Devuelve todos los permisos disponibles en el sistema")
    public ResponseEntity<List<PermissionResponse>> permissions() {
        List<PermissionResponse> response = roleAdminService.listPermissions();
        return ResponseEntity.ok(response);
    }
}
