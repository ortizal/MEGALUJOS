package ec.com.megalujos.showcase.application;

import ec.com.megalujos.catalog.domain.MediaFileRepository;
import ec.com.megalujos.catalog.domain.ProductVariantRepository;
import ec.com.megalujos.common.audit.AuditService;
import ec.com.megalujos.common.exception.BusinessRuleException;
import ec.com.megalujos.common.exception.ResourceNotFoundException;
import ec.com.megalujos.showcase.domain.*;
import ec.com.megalujos.showcase.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** CRUD administrativo de banners, showcases y orden de productos de la vitrina. */
@Service
public class ShowcaseAdminService {

    private final BannerRepository bannerRepository;
    private final ShowcaseRepository showcaseRepository;
    private final ShowcaseProductRepository showcaseProductRepository;
    private final MediaFileRepository mediaFileRepository;
    private final ProductVariantRepository variantRepository;
    private final AuditService auditService;

    public ShowcaseAdminService(BannerRepository bannerRepository,
                                ShowcaseRepository showcaseRepository,
                                ShowcaseProductRepository showcaseProductRepository,
                                MediaFileRepository mediaFileRepository,
                                ProductVariantRepository variantRepository,
                                AuditService auditService) {
        this.bannerRepository = bannerRepository;
        this.showcaseRepository = showcaseRepository;
        this.showcaseProductRepository = showcaseProductRepository;
        this.mediaFileRepository = mediaFileRepository;
        this.variantRepository = variantRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<BannerResponseDto> listBanners() {
        return bannerRepository.findAll().stream().map(BannerResponseDto::from).toList();
    }

    @Transactional
    public BannerResponseDto createBanner(BannerRequest request, UUID actorId, String ipAddress) {
        validateBannerWindow(request.getStartsAt(), request.getEndsAt());
        if (!mediaFileRepository.existsById(request.getMediaId())) {
            throw new ResourceNotFoundException("Archivo media", request.getMediaId());
        }
        Banner banner = new Banner();
        applyBanner(banner, request);
        Banner saved = bannerRepository.save(banner);
        auditService.record(actorId, "BANNER_CREATED", "Banner", saved.getId(), request, ipAddress);
        return BannerResponseDto.from(saved);
    }

    @Transactional
    public BannerResponseDto updateBanner(UUID id, BannerRequest request, UUID actorId, String ipAddress) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner", id));
        validateBannerWindow(request.getStartsAt(), request.getEndsAt());
        if (!mediaFileRepository.existsById(request.getMediaId())) {
            throw new ResourceNotFoundException("Archivo media", request.getMediaId());
        }
        applyBanner(banner, request);
        Banner saved = bannerRepository.save(banner);
        auditService.record(actorId, "BANNER_UPDATED", "Banner", saved.getId(), request, ipAddress);
        return BannerResponseDto.from(saved);
    }

    @Transactional
    public BannerResponseDto toggleBanner(UUID id, boolean active, UUID actorId, String ipAddress) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner", id));
        banner.setActive(active);
        Banner saved = bannerRepository.save(banner);
        auditService.record(actorId, active ? "BANNER_ACTIVATED" : "BANNER_DEACTIVATED",
                "Banner", saved.getId(), null, ipAddress);
        return BannerResponseDto.from(saved);
    }

    private void applyBanner(Banner banner, BannerRequest request) {
        banner.setTitle(request.getTitle());
        banner.setSubtitle(request.getSubtitle());
        banner.setMediaId(request.getMediaId());
        banner.setLinkUrl(request.getLinkUrl());
        banner.setStartsAt(request.getStartsAt());
        banner.setEndsAt(request.getEndsAt());
        banner.setSortOrder(request.getSortOrder());
    }

    private void validateBannerWindow(OffsetDateTime startsAt, OffsetDateTime endsAt) {
        if (startsAt != null && endsAt != null && !endsAt.isAfter(startsAt)) {
            throw new BusinessRuleException("INVALID_BANNER_WINDOW",
                    "La fecha de fin del banner debe ser posterior a la fecha de inicio");
        }
    }

    @Transactional(readOnly = true)
    public List<ShowcaseResponseDto> listShowcases() {
        return showcaseRepository.findAll().stream().map(ShowcaseResponseDto::from).toList();
    }

    /** Upsert por code: crea o actualiza el showcase (título, descripción, autoplay, activo). */
    @Transactional
    public ShowcaseResponseDto upsertShowcase(UpsertShowcaseRequest request, UUID actorId, String ipAddress) {
        Showcase showcase = showcaseRepository.findByCodeAndActiveTrue(request.getCode())
                .or(() -> showcaseRepository.findAll().stream()
                        .filter(s -> s.getCode().equals(request.getCode()))
                        .findFirst())
                .orElseGet(() -> {
                    Showcase created = new Showcase();
                    created.setCode(request.getCode());
                    return created;
                });
        showcase.setTitle(request.getTitle());
        showcase.setDescription(request.getDescription());
        showcase.setAutoplaySeconds(request.getAutoplaySeconds());
        showcase.setActive(request.isActive());
        Showcase saved = showcaseRepository.save(showcase);
        auditService.record(actorId, "SHOWCASE_UPSERTED", "Showcase", saved.getId(), request, ipAddress);
        return ShowcaseResponseDto.from(saved);
    }

    /** Reemplaza por completo la lista ordenada de productos del showcase. */
    @Transactional
    public ShowcaseResponseDto replaceShowcaseProducts(String code, ShowcaseProductsRequest request,
                                                       UUID actorId, String ipAddress) {
        Showcase showcase = showcaseRepository.findAll().stream()
                .filter(s -> s.getCode().equals(code))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Showcase (code)", code));

        List<ShowcaseProduct> existing = showcaseProductRepository.findByIdShowcaseIdOrderBySortOrderAsc(showcase.getId());
        showcaseProductRepository.deleteAll(existing);
        showcaseProductRepository.flush();

        List<ShowcaseProduct> replacement = new ArrayList<>();
        int order = 1;
        for (UUID variantId : request.getVariantIds()) {
            if (!variantRepository.existsById(variantId)) {
                throw new ResourceNotFoundException("Variante", variantId);
            }
            replacement.add(new ShowcaseProduct(new ShowcaseProductId(showcase.getId(), variantId), order++));
        }
        showcaseProductRepository.saveAll(replacement);

        auditService.record(actorId, "SHOWCASE_PRODUCTS_REPLACED", "Showcase", showcase.getId(),
                request.getVariantIds(), ipAddress);
        return ShowcaseResponseDto.from(showcase);
    }
}
