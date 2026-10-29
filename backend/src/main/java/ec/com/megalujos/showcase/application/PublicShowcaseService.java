package ec.com.megalujos.showcase.application;

import ec.com.megalujos.catalog.domain.ProductVariant;
import ec.com.megalujos.catalog.domain.ProductVariantRepository;
import ec.com.megalujos.common.exception.ResourceNotFoundException;
import ec.com.megalujos.inventory.application.StockService;
import ec.com.megalujos.showcase.domain.*;
import ec.com.megalujos.showcase.dto.CatalogInquiryRequest;
import ec.com.megalujos.showcase.dto.PublicPromotionDto;
import ec.com.megalujos.showcase.dto.TvShowcaseDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Endpoints públicos de vitrina: modo TV (solo datos públicos de productos
 * publicados y promociones vigentes) y registro de consultas de catálogo con
 * rate-limit manual por IP (ventana deslizante de 1 minuto, máx. 10 por IP).
 */
@Service
public class PublicShowcaseService {

    private static final int INQUIRY_RATE_LIMIT = 10;
    private static final long INQUIRY_WINDOW_SECONDS = 60;

    private final ShowcaseRepository showcaseRepository;
    private final ShowcaseProductRepository showcaseProductRepository;
    private final ProductVariantRepository variantRepository;
    private final PromotionRepository promotionRepository;
    private final CatalogInquiryRepository catalogInquiryRepository;
    private final StockService stockService;

    private final Map<String, ConcurrentLinkedDeque<Instant>> inquiryHits = new ConcurrentHashMap<>();

    public PublicShowcaseService(ShowcaseRepository showcaseRepository,
                                 ShowcaseProductRepository showcaseProductRepository,
                                 ProductVariantRepository variantRepository,
                                 PromotionRepository promotionRepository,
                                 CatalogInquiryRepository catalogInquiryRepository,
                                 StockService stockService) {
        this.showcaseRepository = showcaseRepository;
        this.showcaseProductRepository = showcaseProductRepository;
        this.variantRepository = variantRepository;
        this.promotionRepository = promotionRepository;
        this.catalogInquiryRepository = catalogInquiryRepository;
        this.stockService = stockService;
    }

    @Transactional(readOnly = true)
    public TvShowcaseDto getTvShowcase() {
        TvShowcaseDto dto = new TvShowcaseDto();
        dto.setAutoplaySeconds(7);

        Optional<Showcase> showcase = showcaseRepository.findByCodeAndActiveTrue("HOME_FEATURED");
        if (showcase.isPresent()) {
            dto.setAutoplaySeconds(showcase.get().getAutoplaySeconds());
            Set<UUID> seen = new HashSet<>();
            for (ShowcaseProduct sp : showcaseProductRepository
                    .findByIdShowcaseIdOrderBySortOrderAsc(showcase.get().getId())) {
                UUID variantId = sp.getId().getVariantId();
                if (!seen.add(variantId)) {
                    continue;
                }
                variantRepository.findById(variantId).ifPresent(variant -> {
                    if (variant.isActive() && variant.getProduct().isActive() && variant.getProduct().isPublished()) {
                        dto.getItems().add(toTvItem(variant));
                    }
                });
            }
        }

        dto.setPromos(promotionRepository.findCurrentPromotions(java.time.OffsetDateTime.now()).stream()
                .map(PublicPromotionDto::from)
                .toList());
        return dto;
    }

    private TvShowcaseDto.TvItemDto toTvItem(ProductVariant variant) {
        TvShowcaseDto.TvItemDto item = new TvShowcaseDto.TvItemDto();
        item.setId(variant.getId());
        item.setName(variant.getProduct().getName());
        item.setSku(variant.getSku());
        item.setPrice(variant.getSalePrice());
        item.setCategory(variant.getProduct().getCategory() != null
                ? variant.getProduct().getCategory().getName() : null);

        item.setImageUrl(variant.getProduct().getImages().stream()
                .filter(pi -> pi.isPrimary())
                .findFirst()
                .or(() -> variant.getProduct().getImages().stream().findFirst())
                .map(pi -> "/uploads/" + pi.getMediaFile().getStorageKey())
                .orElse(null));

        if ("SERVICE".equalsIgnoreCase(variant.getProduct().getProductType()) || !variant.getProduct().isTrackStock()) {
            item.setAvailability("AVAILABLE");
        } else {
            BigDecimal available = stockService.getAvailableStock(variant.getId(), null);
            if (available.compareTo(BigDecimal.ZERO) <= 0) {
                item.setAvailability("OUT_OF_STOCK");
            } else if (available.compareTo(variant.getMinStock()) <= 0) {
                item.setAvailability("LOW_STOCK");
            } else {
                item.setAvailability("AVAILABLE");
            }
        }
        return item;
    }

    @Transactional
    public void registerInquiry(CatalogInquiryRequest request, String userAgent, String ipAddress) {
        checkRateLimit(ipAddress);
        if (request.getVariantId() != null && !variantRepository.existsById(request.getVariantId())) {
            throw new ResourceNotFoundException("Variante", request.getVariantId());
        }
        CatalogInquiry inquiry = new CatalogInquiry();
        inquiry.setVariantId(request.getVariantId());
        inquiry.setCustomerPhone(request.getCustomerPhone());
        inquiry.setUserAgent(userAgent != null && userAgent.length() > 2000
                ? userAgent.substring(0, 2000) : userAgent);
        catalogInquiryRepository.save(inquiry);
    }

    /** Rate-limit manual simple por IP: ventana deslizante en memoria. */
    private void checkRateLimit(String ipAddress) {
        String key = ipAddress != null ? ipAddress : "unknown";
        Instant now = Instant.now();
        ConcurrentLinkedDeque<Instant> hits = inquiryHits.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
        synchronized (hits) {
            while (!hits.isEmpty() && hits.peekFirst().isBefore(now.minusSeconds(INQUIRY_WINDOW_SECONDS))) {
                hits.pollFirst();
            }
            if (hits.size() >= INQUIRY_RATE_LIMIT) {
                throw new ec.com.megalujos.common.exception.BusinessRuleException(
                        "RATE_LIMITED", "Demasiadas consultas en breve intervalo; intente más tarde");
            }
            hits.addLast(now);
        }
    }
}
