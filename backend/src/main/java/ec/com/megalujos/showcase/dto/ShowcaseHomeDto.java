package ec.com.megalujos.showcase.dto;

import ec.com.megalujos.catalog.dto.ProductPublicDto;

import java.util.List;

public class ShowcaseHomeDto {

    private List<BannerResponseDto> banners;
    private String showcaseTitle;
    private int autoplaySeconds;
    private List<ProductPublicDto> featuredProducts;

    public ShowcaseHomeDto() {
    }

    public List<BannerResponseDto> getBanners() {
        return banners;
    }

    public void setBanners(List<BannerResponseDto> banners) {
        this.banners = banners;
    }

    public String getShowcaseTitle() {
        return showcaseTitle;
    }

    public void setShowcaseTitle(String showcaseTitle) {
        this.showcaseTitle = showcaseTitle;
    }

    public int getAutoplaySeconds() {
        return autoplaySeconds;
    }

    public void setAutoplaySeconds(int autoplaySeconds) {
        this.autoplaySeconds = autoplaySeconds;
    }

    public List<ProductPublicDto> getFeaturedProducts() {
        return featuredProducts;
    }

    public void setFeaturedProducts(List<ProductPublicDto> featuredProducts) {
        this.featuredProducts = featuredProducts;
    }
}
