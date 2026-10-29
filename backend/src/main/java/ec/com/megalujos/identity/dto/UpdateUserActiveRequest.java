package ec.com.megalujos.identity.dto;

import jakarta.validation.constraints.NotNull;

public class UpdateUserActiveRequest {

    @NotNull(message = "El estado activo es obligatorio")
    private Boolean active;

    public UpdateUserActiveRequest() {
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
