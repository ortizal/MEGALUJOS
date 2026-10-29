package ec.com.megalujos.showcase.api;

import ec.com.megalujos.common.security.UserPrincipal;
import ec.com.megalujos.showcase.application.ShowcaseAdminService;
import ec.com.megalujos.showcase.dto.*;
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
@RequestMapping("/api/v1")
@Tag(name = "Admin Vitrina", description = "Gestión administrativa de banners, showcases y orden de productos de la vitrina")
@PreAuthorize("hasAuthority('showcase:manage') or hasRole('ADMIN')")
public class ShowcaseAdminController {

    private final ShowcaseAdminService showcaseAdminService;

    public ShowcaseAdminController(ShowcaseAdminService showcaseAdminService) {
        this.showcaseAdminService = showcaseAdminService;
    }

    @GetMapping("/banners")
    @Operation(summary = "Listar banners", description = "Lista todos los banners configurados (activos e inactivos)")
    public ResponseEntity<List<BannerResponseDto>> listBanners() {
        return ResponseEntity.ok(showcaseAdminService.listBanners());
    }

    @PostMapping("/banners")
    @Operation(summary = "Crear banner", description = "Crea un banner con media, enlace opcional y ventana de fechas; se valida que endsAt > startsAt")
    public ResponseEntity<BannerResponseDto> createBanner(
            @Valid @RequestBody BannerRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {
        UUID actorId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(showcaseAdminService.createBanner(request, actorId, httpRequest.getRemoteAddr()));
    }

    @PutMapping("/banners/{id}")
    @Operation(summary = "Actualizar banner", description = "Actualiza los datos de un banner existente")
    public ResponseEntity<BannerResponseDto> updateBanner(
            @PathVariable UUID id,
            @Valid @RequestBody BannerRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {
        UUID actorId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(showcaseAdminService.updateBanner(id, request, actorId, httpRequest.getRemoteAddr()));
    }

    @PatchMapping("/banners/{id}/active")
    @Operation(summary = "Activar/desactivar banner", description = "Cambia el estado activo de un banner")
    public ResponseEntity<BannerResponseDto> toggleBanner(
            @PathVariable UUID id,
            @RequestParam boolean active,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {
        UUID actorId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(showcaseAdminService.toggleBanner(id, active, actorId, httpRequest.getRemoteAddr()));
    }

    @GetMapping("/showcases")
    @Operation(summary = "Listar showcases", description = "Lista los showcases con su autoplaySeconds (3..60) y estado")
    public ResponseEntity<List<ShowcaseResponseDto>> listShowcases() {
        return ResponseEntity.ok(showcaseAdminService.listShowcases());
    }

    @PutMapping("/showcases")
    @Operation(summary = "Crear/actualizar showcase", description = "Upsert por code del showcase con título, descripción, autoplaySeconds (3..60) y estado")
    public ResponseEntity<ShowcaseResponseDto> upsertShowcase(
            @Valid @RequestBody UpsertShowcaseRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {
        UUID actorId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(showcaseAdminService.upsertShowcase(request, actorId, httpRequest.getRemoteAddr()));
    }

    @PutMapping("/showcases/{code}/products")
    @Operation(summary = "Reemplazar productos del showcase", description = "Reemplaza por completo la lista ordenada de variantes del showcase por code")
    public ResponseEntity<ShowcaseResponseDto> replaceProducts(
            @PathVariable String code,
            @Valid @RequestBody ShowcaseProductsRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {
        UUID actorId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(showcaseAdminService.replaceShowcaseProducts(code, request, actorId, httpRequest.getRemoteAddr()));
    }
}
