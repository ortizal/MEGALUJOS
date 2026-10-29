package ec.com.megalujos.showcase.dto;

import ec.com.megalujos.showcase.domain.Promotion;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class PromotionResponseDto {

    private UUID id;
    private String name;
    private String description;
    private String promoType;
    private BigDecimal value;
    private OffsetDateTime startsAt;
    private OffsetDateTime endsAt;
    private boolean active;
    private UUID productId;
    private UUID categoryId;

    public PromotionResponseDto() {
    }

    public static PromotionResponseDto from(Promotion promo) {
        PromotionResponseDto dto = new PromotionResponseDto();
        dto.setId(promo.getId());
        dto.setName(promo.getName());
        dto.setDescription(promo.getDescription());
        dto.setPromoType(promo.getPromoType());
        dto.setValue(promo.getValue());
        dto.setStartsAt(promo.getStartsAt());
        dto.setEndsAt(promo.getEndsAt());
        dto.setActive(promo.isActive());
        dto.setProductId(promo.getProductId());
        dto.setCategoryId(promo.getCategoryId());
        return dto;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPromoType() {
        return promoType;
    }

    public void setPromoType(String promoType) {
        this.promoType = promoType;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }
}
