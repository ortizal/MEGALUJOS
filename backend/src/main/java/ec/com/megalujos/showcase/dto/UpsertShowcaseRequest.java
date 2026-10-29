package ec.com.megalujos.showcase.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpsertShowcaseRequest {

    @NotBlank
    @Size(max = 60)
    private String code;

    @NotBlank
    @Size(max = 180)
    private String title;

    @Size(max = 2000)
    private String description;

    @Min(3)
    @Max(60)
    private int autoplaySeconds = 7;

    private boolean active = true;

    public UpsertShowcaseRequest() {
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getAutoplaySeconds() {
        return autoplaySeconds;
    }

    public void setAutoplaySeconds(int autoplaySeconds) {
        this.autoplaySeconds = autoplaySeconds;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
