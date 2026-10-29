package ec.com.megalujos.showcase.application;

import ec.com.megalujos.catalog.application.ProductService;
import ec.com.megalujos.catalog.domain.ProductVariant;
import ec.com.megalujos.catalog.domain.ProductVariantRepository;
import ec.com.megalujos.catalog.dto.ProductPublicDto;
import ec.com.megalujos.showcase.domain.*;
import ec.com.megalujos.showcase.dto.BannerResponseDto;
import ec.com.megalujos.showcase.dto.ShowcaseHomeDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ShowcaseService {

    private final BannerRepository bannerRepository;
    private final ShowcaseRepository showcaseRepository;
    private final ShowcaseProductRepository showcaseProductRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductService productService;

    public ShowcaseService(BannerRepository bannerRepository,
                           ShowcaseRepository showcaseRepository,
                           ShowcaseProductRepository showcaseProductRepository,
                           ProductVariantRepository variantRepository,
                           ProductService productService) {
        this.bannerRepository = bannerRepository;
        this.showcaseRepository = showcaseRepository;
        this.showcaseProductRepository = showcaseProductRepository;
        this.variantRepository = variantRepository;
        this.productService = productService;
    }

    @Transactional(readOnly = true)
    public ShowcaseHomeDto getHomeShowcase() {
        ShowcaseHomeDto dto = new ShowcaseHomeDto();
        OffsetDateTime now = OffsetDateTime.now();
        dto.setBanners(bannerRepository.findByActiveTrueOrderBySortOrderAsc().stream()
                .filter(b -> isCurrentlyVisible(b, now))
                .map(BannerResponseDto::from)
                .toList());

        Optional<Showcase> optShowcase = showcaseRepository.findByCodeAndActiveTrue("HOME_FEATURED");
        if (optShowcase.isPresent()) {
            Showcase showcase = optShowcase.get();
            dto.setShowcaseTitle(showcase.getTitle());
            dto.setAutoplaySeconds(showcase.getAutoplaySeconds());

            List<ShowcaseProduct> spList = showcaseProductRepository.findByIdShowcaseIdOrderBySortOrderAsc(showcase.getId());
            List<ProductPublicDto> products = new ArrayList<>();

            for (ShowcaseProduct sp : spList) {
                variantRepository.findById(sp.getId().getVariantId()).ifPresent(variant -> {
                    if (variant.getProduct().isActive() && variant.getProduct().isPublished()) {
                        products.add(productService.mapToPublicDto(variant.getProduct()));
                    }
                });
            }
            dto.setFeaturedProducts(products);
        } else {
            dto.setShowcaseTitle("Destacados Megalujos");
            dto.setAutoplaySeconds(6);
            dto.setFeaturedProducts(new ArrayList<>());
        }

        return dto;
    }

    /** Un banner es visible si su ventana [startsAt, endsAt] (ambos opcionales) contiene el instante actual. */
    private boolean isCurrentlyVisible(Banner banner, OffsetDateTime now) {
        if (banner.getStartsAt() != null && now.isBefore(banner.getStartsAt())) {
            return false;
        }
        return banner.getEndsAt() == null || !now.isAfter(banner.getEndsAt());
    }
}
