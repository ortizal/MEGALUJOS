package ec.com.megalujos.showcase.api;

import ec.com.megalujos.showcase.application.PromotionService;
import ec.com.megalujos.showcase.application.PublicShowcaseService;
import ec.com.megalujos.showcase.dto.CatalogInquiryRequest;
import ec.com.megalujos.showcase.dto.PublicPromotionDto;
import ec.com.megalujos.showcase.dto.TvShowcaseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/public")
@Tag(name = "Vitrina Pública", description = "Endpoints públicos: promociones vigentes, registro de consultas de catálogo y modo TV")
public class PublicShowcaseController {

    private final PromotionService promotionService;
    private final PublicShowcaseService publicShowcaseService;

    public PublicShowcaseController(PromotionService promotionService,
                                    PublicShowcaseService publicShowcaseService) {
        this.promotionService = promotionService;
        this.publicShowcaseService = publicShowcaseService;
    }

    @GetMapping("/promotions")
    @Operation(summary = "Promociones vigentes", description = "Lista promociones activas cuya ventana de fechas incluye el momento actual")
    public ResponseEntity<List<PublicPromotionDto>> currentPromotions() {
        return ResponseEntity.ok(promotionService.listCurrent());
    }

    @PostMapping("/catalog/inquiries")
    @Operation(summary = "Registrar consulta de catálogo", description = "Registra una consulta de producto (origen WhatsApp u otro); público con rate-limit por IP")
    public ResponseEntity<Map<String, String>> registerInquiry(
            @Valid @RequestBody CatalogInquiryRequest request,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            HttpServletRequest httpRequest) {
        publicShowcaseService.registerInquiry(request, userAgent, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(Map.of("status", "RECORDED"));
    }

    @GetMapping("/showcases/tv")
    @Operation(summary = "Modo TV", description = "Retorna autoplaySeconds, items de productos publicados (sin costos ni datos privados) y promociones vigentes")
    public ResponseEntity<TvShowcaseDto> tvShowcase() {
        return ResponseEntity.ok(publicShowcaseService.getTvShowcase());
    }
}
