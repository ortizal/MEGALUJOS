package ec.com.megalujos.catalog.application;

import ec.com.megalujos.catalog.domain.Brand;
import ec.com.megalujos.catalog.domain.BrandRepository;
import ec.com.megalujos.catalog.domain.MediaFileRepository;
import ec.com.megalujos.catalog.dto.BrandRequest;
import ec.com.megalujos.catalog.dto.BrandResponse;
import ec.com.megalujos.common.audit.AuditService;
import ec.com.megalujos.common.exception.BusinessRuleException;
import ec.com.megalujos.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BrandAdminService {

    private final BrandRepository brandRepository;
    private final MediaFileRepository mediaFileRepository;
    private final AuditService auditService;

    public BrandAdminService(
            BrandRepository brandRepository,
            MediaFileRepository mediaFileRepository,
            AuditService auditService
    ) {
        this.brandRepository = brandRepository;
        this.mediaFileRepository = mediaFileRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<BrandResponse> list(String q) {
        List<Brand> brands = (q == null || q.isBlank())
                ? brandRepository.findAllByOrderByNameAsc()
                : brandRepository.findByNameContainingIgnoreCaseOrderByNameAsc(q.trim());
        return brands.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public BrandResponse create(BrandRequest request, UUID actorId, String ipAddress) {
        String name = request.getName().trim();
        validateNameAvailable(name, null);

        Brand brand = new Brand(name);
        brand.setLogoMediaId(validateMedia(request.getLogoMediaId()));
        brand.setActive(request.getActive() == null || request.getActive());

        Brand saved = brandRepository.save(brand);
        auditService.record(actorId, "BRAND_CREATED", "Brand", saved.getId(),
                Map.of("name", saved.getName()), ipAddress);
        return toResponse(saved);
    }

    @Transactional
    public BrandResponse update(UUID id, BrandRequest request, UUID actorId, String ipAddress) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Marca", id));

        String name = request.getName().trim();
        if (!brand.getName().equalsIgnoreCase(name)) {
            validateNameAvailable(name, null);
        }
        brand.setName(name);
        brand.setLogoMediaId(validateMedia(request.getLogoMediaId()));
        if (request.getActive() != null) {
            brand.setActive(request.getActive());
        }

        Brand saved = brandRepository.save(brand);
        auditService.record(actorId, "BRAND_UPDATED", "Brand", saved.getId(),
                Map.of("name", saved.getName()), ipAddress);
        return toResponse(saved);
    }

    @Transactional
    public BrandResponse setActive(UUID id, boolean active, UUID actorId, String ipAddress) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Marca", id));
        brand.setActive(active);
        Brand saved = brandRepository.save(brand);
        auditService.record(actorId, "BRAND_UPDATED", "Brand", saved.getId(),
                Map.of("name", saved.getName(), "active", active), ipAddress);
        return toResponse(saved);
    }

    private void validateNameAvailable(String name, UUID currentId) {
        brandRepository.findByNameIgnoreCase(name).ifPresent(existing -> {
            if (currentId == null || !currentId.equals(existing.getId())) {
                throw new BusinessRuleException("BRAND_NAME_DUPLICATED",
                        "Ya existe una marca con el nombre [" + name + "]");
            }
        });
    }

    private UUID validateMedia(UUID mediaId) {
        if (mediaId == null) {
            return null;
        }
        mediaFileRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Archivo", mediaId));
        return mediaId;
    }

    private BrandResponse toResponse(Brand brand) {
        BrandResponse dto = new BrandResponse();
        dto.setId(brand.getId());
        dto.setName(brand.getName());
        dto.setLogoMediaId(brand.getLogoMediaId());
        if (brand.getLogoMediaId() != null) {
            mediaFileRepository.findById(brand.getLogoMediaId())
                    .ifPresent(media -> dto.setLogoUrl("/uploads/" + media.getStorageKey()));
        }
        dto.setActive(brand.isActive());
        dto.setCreatedAt(brand.getCreatedAt());
        return dto;
    }
}
