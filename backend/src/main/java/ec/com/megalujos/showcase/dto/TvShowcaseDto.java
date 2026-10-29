package ec.com.megalujos.showcase.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Respuesta del modo TV: solo datos públicos de productos publicados y promos vigentes. */
public class TvShowcaseDto {

    private int autoplaySeconds;
    private List<TvItemDto> items = new ArrayList<>();
    private List<PublicPromotionDto> promos = new ArrayList<>();

    public TvShowcaseDto() {
    }

    public int getAutoplaySeconds() {
        return autoplaySeconds;
    }

    public void setAutoplaySeconds(int autoplaySeconds) {
        this.autoplaySeconds = autoplaySeconds;
    }

    public List<TvItemDto> getItems() {
        return items;
    }

    public void setItems(List<TvItemDto> items) {
        this.items = items;
    }

    public List<PublicPromotionDto> getPromos() {
        return promos;
    }

    public void setPromos(List<PublicPromotionDto> promos) {
        this.promos = promos;
    }

    public static class TvItemDto {
        private UUID id;
        private String name;
        private String sku;
        private BigDecimal price;
        private String imageUrl;
        private String availability;
        private String category;

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

        public String getSku() {
            return sku;
        }

        public void setSku(String sku) {
            this.sku = sku;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }

        public String getAvailability() {
            return availability;
        }

        public void setAvailability(String availability) {
            this.availability = availability;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }
    }
}
