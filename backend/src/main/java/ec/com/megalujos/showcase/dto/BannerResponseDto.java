package ec.com.megalujos.showcase.dto;

import ec.com.megalujos.showcase.domain.Banner;

import java.time.OffsetDateTime;
import java.util.UUID;

public class BannerResponseDto {

    private UUID id;
    private String title;
    private String subtitle;
    private UUID mediaId;
    private String linkUrl;
    private OffsetDateTime startsAt;
    private OffsetDateTime endsAt;
    private int sortOrder;
    private boolean active;

    public BannerResponseDto() {
    }

    public static BannerResponseDto from(Banner banner) {
        BannerResponseDto dto = new BannerResponseDto();
        dto.setId(banner.getId());
        dto.setTitle(banner.getTitle());
        dto.setSubtitle(banner.getSubtitle());
        dto.setMediaId(banner.getMediaId());
        dto.setLinkUrl(banner.getLinkUrl());
        dto.setStartsAt(banner.getStartsAt());
        dto.setEndsAt(banner.getEndsAt());
        dto.setSortOrder(banner.getSortOrder());
        dto.setActive(banner.isActive());
        return dto;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public UUID getMediaId() {
        return mediaId;
    }

    public void setMediaId(UUID mediaId) {
        this.mediaId = mediaId;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public void setLinkUrl(String linkUrl) {
        this.linkUrl = linkUrl;
    }

    public OffsetDateTime getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(OffsetDateTime startsAt) {
        this.startsAt = startsAt;
    }

    public OffsetDateTime getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(OffsetDateTime endsAt) {
        this.endsAt = endsAt;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
