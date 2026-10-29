package ec.com.megalujos.common.files;

import ec.com.megalujos.catalog.domain.MediaFile;
import ec.com.megalujos.common.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/media")
@Tag(name = "Archivos y Media", description = "Subida segura de fotos de productos y archivos de servicio")
public class MediaController {

    private final FileStorageService fileStorageService;

    public MediaController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('catalog:write', 'services:manage', 'showcase:manage', 'settings:manage') or hasRole('ADMIN')")
    @Operation(summary = "Subir archivo", description = "Valida MIME real, extensión y tamaño; devuelve el mediaId para asociarlo a productos o servicios")
    public ResponseEntity<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "isPublic", defaultValue = "true") boolean isPublic,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        MediaFile stored = fileStorageService.store(file, isPublic);
        Map<String, Object> response = new HashMap<>();
        response.put("id", stored.getId());
        response.put("storageKey", stored.getStorageKey());
        response.put("originalName", stored.getOriginalName());
        response.put("contentType", stored.getContentType());
        response.put("sizeBytes", stored.getSizeBytes());
        response.put("sha256", stored.getSha256());
        response.put("url", "/uploads/" + stored.getStorageKey());
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Metadatos de archivo", description = "Obtiene los metadatos del archivo por id")
    public ResponseEntity<Map<String, Object>> metadata(@PathVariable UUID id) {
        MediaFile media = fileStorageService.getRequired(id);
        Map<String, Object> response = new HashMap<>();
        response.put("id", media.getId());
        response.put("storageKey", media.getStorageKey());
        response.put("originalName", media.getOriginalName());
        response.put("contentType", media.getContentType());
        response.put("sizeBytes", media.getSizeBytes());
        response.put("public", media.isPublic());
        response.put("url", "/uploads/" + media.getStorageKey());
        return ResponseEntity.ok(response);
    }
}
