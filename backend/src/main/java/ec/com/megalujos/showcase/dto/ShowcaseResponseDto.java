package ec.com.megalujos.showcase.dto;

import ec.com.megalujos.showcase.domain.Showcase;

import java.util.UUID;

public class ShowcaseResponseDto {

    private UUID id;
    private String code;
    private String title;
    private String description;
    private int autoplaySeconds;
    private boolean active;

    public ShowcaseResponseDto() {
    }

    public static ShowcaseResponseDto from(Showcase showcase) {
        ShowcaseResponseDto dto = new ShowcaseResponseDto();
        dto.setId(showcase.getId());
        dto.setCode(showcase.getCode());
        dto.setTitle(showcase.getTitle());
        dto.setDescription(showcase.getDescription());
        dto.setAutoplaySeconds(showcase.getAutoplaySeconds());
        dto.setActive(showcase.isActive());
        return dto;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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
