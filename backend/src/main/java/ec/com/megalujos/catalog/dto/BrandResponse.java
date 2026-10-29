package ec.com.megalujos.catalog.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class BrandResponse {

    private UUID id;
    private String name;
    private UUID logoMediaId;
    private String logoUrl;
    private boolean active;
    private OffsetDateTime createdAt;

    public BrandResponse() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getLogoMediaId() {
        return logoMediaId;
    }

    public void setLogoMediaId(UUID logoMediaId) {
        this.logoMediaId = logoMediaId;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
