package ec.com.megalujos.showcase.application;

import ec.com.megalujos.common.audit.AuditService;
import ec.com.megalujos.common.exception.BusinessRuleException;
import ec.com.megalujos.common.exception.ResourceNotFoundException;
import ec.com.megalujos.showcase.domain.Promotion;
import ec.com.megalujos.showcase.domain.PromotionRepository;
import ec.com.megalujos.showcase.dto.PromotionRequest;
import ec.com.megalujos.showcase.dto.PromotionResponseDto;
import ec.com.megalujos.showcase.dto.PublicPromotionDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final AuditService auditService;

    public PromotionService(PromotionRepository promotionRepository, AuditService auditService) {
        this.promotionRepository = promotionRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<PromotionResponseDto> list() {
        return promotionRepository.findAll().stream().map(PromotionResponseDto::from).toList();
    }

    @Transactional
    public PromotionResponseDto create(PromotionRequest request, UUID actorId, String ipAddress) {
        validate(request);
        Promotion promo = new Promotion();
        apply(promo, request);
        Promotion saved = promotionRepository.save(promo);
        auditService.record(actorId, "PROMOTION_CREATED", "Promotion", saved.getId(), request, ipAddress);
        return PromotionResponseDto.from(saved);
    }

    @Transactional
    public PromotionResponseDto update(UUID id, PromotionRequest request, UUID actorId, String ipAddress) {
        Promotion promo = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promoción", id));
        validate(request);
        apply(promo, request);
        Promotion saved = promotionRepository.save(promo);
        auditService.record(actorId, "PROMOTION_UPDATED", "Promotion", saved.getId(), request, ipAddress);
        return PromotionResponseDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<PublicPromotionDto> listCurrent() {
        return promotionRepository.findCurrentPromotions(OffsetDateTime.now()).stream()
                .map(PublicPromotionDto::from)
                .toList();
    }

    private void validate(PromotionRequest request) {
        if (!request.getEndsAt().isAfter(request.getStartsAt())) {
            throw new BusinessRuleException("INVALID_PROMOTION_WINDOW",
                    "La fecha de fin de la promoción debe ser posterior a la fecha de inicio");
        }
        if ("PERCENT".equals(request.getPromoType())
                && request.getValue().compareTo(new java.math.BigDecimal("100")) > 0) {
            throw new BusinessRuleException("INVALID_PROMOTION_VALUE",
                    "El valor de una promoción PERCENT no puede superar 100");
        }
    }

    private void apply(Promotion promo, PromotionRequest request) {
        promo.setName(request.getName());
        promo.setDescription(request.getDescription());
        promo.setPromoType(request.getPromoType());
        promo.setValue(request.getValue());
        promo.setStartsAt(request.getStartsAt());
        promo.setEndsAt(request.getEndsAt());
        promo.setActive(request.isActive());
        promo.setProductId(request.getProductId());
        promo.setCategoryId(request.getCategoryId());
    }
}
