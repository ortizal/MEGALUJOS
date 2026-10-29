package ec.com.megalujos.identity.application;

import ec.com.megalujos.identity.domain.Permission;
import ec.com.megalujos.identity.domain.PermissionRepository;
import ec.com.megalujos.identity.domain.Role;
import ec.com.megalujos.identity.domain.RoleRepository;
import ec.com.megalujos.identity.dto.PermissionResponse;
import ec.com.megalujos.identity.dto.RoleResponse;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class RoleAdminService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RoleAdminService(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> listRoles() {
        List<Role> roles = roleRepository.findAll(Sort.by("code"));
        List<RoleResponse> response = new ArrayList<>();
        for (Role role : roles) {
            response.add(toRoleResponse(role));
        }
        return response;
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> listPermissions() {
        List<Permission> permissions = permissionRepository.findAll(Sort.by("code"));
        List<PermissionResponse> response = new ArrayList<>();
        for (Permission permission : permissions) {
            response.add(toPermissionResponse(permission));
        }
        return response;
    }

    private RoleResponse toRoleResponse(Role role) {
        RoleResponse response = new RoleResponse();
        response.setId(role.getId());
        response.setCode(role.getCode());
        response.setName(role.getName());
        response.setDescription(role.getDescription());

        List<PermissionResponse> permissions = new ArrayList<>();
        for (Permission permission : role.getPermissions()) {
            permissions.add(toPermissionResponse(permission));
        }
        response.setPermissions(permissions);
        return response;
    }

    private PermissionResponse toPermissionResponse(Permission permission) {
        PermissionResponse response = new PermissionResponse();
        response.setId(permission.getId());
        response.setCode(permission.getCode());
        response.setName(permission.getName());
        response.setModule(permission.getModule());
        return response;
    }
}
