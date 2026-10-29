package ec.com.megalujos.showcase.api;

import ec.com.megalujos.common.security.UserPrincipal;
import ec.com.megalujos.showcase.application.PromotionService;
import ec.com.megalujos.showcase.dto.PromotionRequest;
import ec.com.megalujos.showcase.dto.PromotionResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/promotions")
@Tag(name = "Promociones (Admin)", description = "CRUD de promociones de vitrina por producto o categoría")
@PreAuthorize("hasAuthority('showcase:manage') or hasRole('ADMIN')")
public class PromotionController {

    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @GetMapping
    @Operation(summary = "Listar promociones", description = "Lista todas las promociones configuradas")
    public ResponseEntity<List<PromotionResponseDto>> list() {
        return ResponseEntity.ok(promotionService.list());
    }

    @PostMapping
    @Operation(summary = "Crear promoción", description = "Crea una promoción PERCENT, AMOUNT o FIXED_PRICE con ventana de vigencia")
    public ResponseEntity<PromotionResponseDto> create(
            @Valid @RequestBody PromotionRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {
        UUID actorId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(promotionService.create(request, actorId, httpRequest.getRemoteAddr()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar promoción", description = "Actualiza una promoción existente")
    public ResponseEntity<PromotionResponseDto> update(
            @PathVariable UUID id,
            @Valid @RequestBody PromotionRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {
        UUID actorId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(promotionService.update(id, request, actorId, httpRequest.getRemoteAddr()));
    }
}
