package ec.com.megalujos.showcase.api;

import ec.com.megalujos.showcase.application.ShowcaseService;
import ec.com.megalujos.showcase.dto.ShowcaseHomeDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/showcases")
@Tag(name = "Vitrina y Modo TV", description = "Endpoints para banners, productos destacados y presentación automática en pantallas y Modo TV")
public class ShowcaseController {

    private final ShowcaseService showcaseService;

    public ShowcaseController(ShowcaseService showcaseService) {
        this.showcaseService = showcaseService;
    }

    @GetMapping("/home")
    @Operation(summary = "Obtener contenido del Home y Modo TV", description = "Retorna los banners, destacados y tiempo de rotación para carruseles y Modo TV")
    public ResponseEntity<ShowcaseHomeDto> getHomeShowcase() {
        return ResponseEntity.ok(showcaseService.getHomeShowcase());
    }
}
