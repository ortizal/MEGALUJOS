package ec.com.megalujos.identity.application;

import ec.com.megalujos.common.audit.AuditService;
import ec.com.megalujos.common.exception.BusinessRuleException;
import ec.com.megalujos.common.exception.ResourceNotFoundException;
import ec.com.megalujos.common.web.PageResponse;
import ec.com.megalujos.identity.domain.Permission;
import ec.com.megalujos.identity.domain.Role;
import ec.com.megalujos.identity.domain.RoleRepository;
import ec.com.megalujos.identity.domain.User;
import ec.com.megalujos.identity.domain.UserRepository;
import ec.com.megalujos.identity.dto.CreateUserRequest;
import ec.com.megalujos.identity.dto.ResetPasswordRequest;
import ec.com.megalujos.identity.dto.UpdateUserActiveRequest;
import ec.com.megalujos.identity.dto.UpdateUserRequest;
import ec.com.megalujos.identity.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class UserAdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserAdminService(UserRepository userRepository,
                            RoleRepository roleRepository,
                            PasswordEncoder passwordEncoder,
                            AuditService auditService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(String q, Boolean active, Pageable pageable) {
        String searchTerm = StringUtils.hasText(q) ? q.trim() : null;
        Page<User> page = userRepository.search(searchTerm, active, pageable);
        return PageResponse.from(page, this::toResponse);
    }

    @Transactional
    public UserResponse create(CreateUserRequest request, UUID actorId, String ipAddress) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessRuleException("USERNAME_TAKEN", "El nombre de usuario ya está en uso");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setActive(true);
        user.setRoles(resolveRoles(request.getRoleIds()));

        User saved = userRepository.save(user);

        auditService.record(actorId, "USER_CREATED", "USER", saved.getId(),
                Map.of("username", saved.getUsername()), ipAddress);
        return toResponse(saved);
    }

    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest request, UUID actorId, String ipAddress) {
        User user = findUser(id);

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRoles(resolveRoles(request.getRoleIds()));

        User saved = userRepository.save(user);

        auditService.record(actorId, "USER_UPDATED", "USER", saved.getId(),
                Map.of("username", saved.getUsername()), ipAddress);
        return toResponse(saved);
    }

    @Transactional
    public UserResponse changeActive(UUID id, UpdateUserActiveRequest request, UUID actorId, String ipAddress) {
        User user = findUser(id);

        user.setActive(Boolean.TRUE.equals(request.getActive()));
        User saved = userRepository.save(user);

        auditService.record(actorId, "USER_STATUS_CHANGED", "USER", saved.getId(),
                Map.of("username", saved.getUsername(), "active", saved.isActive()), ipAddress);
        return toResponse(saved);
    }

    @Transactional
    public UserResponse resetPassword(UUID id, ResetPasswordRequest request, UUID actorId, String ipAddress) {
        User user = findUser(id);

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        User saved = userRepository.save(user);

        auditService.record(actorId, "USER_PASSWORD_RESET", "USER", saved.getId(),
                Map.of("username", saved.getUsername()), ipAddress);
        return toResponse(saved);
    }

    private User findUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
    }

    private Set<Role> resolveRoles(List<UUID> roleIds) {
        Set<Role> roles = new HashSet<>();
        for (UUID roleId : roleIds) {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Rol", roleId));
            roles.add(role);
        }
        return roles;
    }

    private UserResponse toResponse(User user) {
        List<String> roles = new ArrayList<>();
        List<String> permissions = new ArrayList<>();

        for (Role role : user.getRoles()) {
            roles.add(role.getCode());
            for (Permission perm : role.getPermissions()) {
                if (!permissions.contains(perm.getCode())) {
                    permissions.add(perm.getCode());
                }
            }
        }

        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setRoles(roles);
        response.setPermissions(permissions);
        return response;
    }
}
