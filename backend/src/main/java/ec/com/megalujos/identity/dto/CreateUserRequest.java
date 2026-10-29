package ec.com.megalujos.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public class CreateUserRequest {

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(max = 80, message = "El nombre de usuario no puede exceder 80 caracteres")
    private String username;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 160, message = "El nombre completo no puede exceder 160 caracteres")
    private String fullName;

    @Email(message = "El correo electrónico no es válido")
    @Size(max = 180, message = "El correo electrónico no puede exceder 180 caracteres")
    private String email;

    @Size(max = 40, message = "El teléfono no puede exceder 40 caracteres")
    private String phone;

    @NotEmpty(message = "Debe asignar al menos un rol")
    private List<UUID> roleIds;

    public CreateUserRequest() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public List<UUID> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<UUID> roleIds) {
        this.roleIds = roleIds;
    }
}
