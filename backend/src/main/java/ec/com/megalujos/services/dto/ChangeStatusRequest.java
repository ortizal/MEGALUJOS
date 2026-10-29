package ec.com.megalujos.services.dto;

import jakarta.validation.constraints.NotBlank;

public class ChangeStatusRequest {

    @NotBlank(message = "El nuevo estado es obligatorio")
    private String status;

    private String notes;

    public ChangeStatusRequest() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
