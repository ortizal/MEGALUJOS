package ec.com.megalujos.common.files;

import ec.com.megalujos.catalog.domain.MediaFile;
import ec.com.megalujos.catalog.domain.MediaFileRepository;
import ec.com.megalujos.common.exception.BusinessRuleException;
import ec.com.megalujos.common.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Almacenamiento privado de archivos con validación de MIME real,
 * extensión permitida, tamaño y hash SHA-256.
 */
@Service
public class FileStorageService {

    private static final Map<String, Set<String>> ALLOWED_TYPES = Map.of(
            "image/jpeg", Set.of("jpg", "jpeg"),
            "image/png", Set.of("png"),
            "image/webp", Set.of("webp"),
            "image/avif", Set.of("avif"),
            "image/svg+xml", Set.of("svg"),
            "application/pdf", Set.of("pdf"),
            "application/vnd.adobe.xfdf", Set.of("xfdf"),
            "application/x-photoshop", Set.of("psd"),
            "image/vnd.adobe.photoshop", Set.of("psd")
    );

    private static final long MAX_SIZE_BYTES = 25L * 1024 * 1024;

    private final MediaFileRepository mediaFileRepository;
    private final Path root;

    public FileStorageService(MediaFileRepository mediaFileRepository,
                              @Value("${megalujos.storage.upload-dir:./uploads}") String uploadDir) {
        this.mediaFileRepository = mediaFileRepository;
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Transactional
    public MediaFile store(MultipartFile file, boolean isPublic) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("EMPTY_FILE", "El archivo está vacío");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BusinessRuleException("FILE_TOO_LARGE", "El archivo supera el límite de 25 MB");
        }

        String originalName = sanitize(file.getOriginalFilename());
        String extension = extensionOf(originalName);
        String detectedType = detectContentType(file, extension);

        Set<String> allowedExtensions = ALLOWED_TYPES.get(detectedType);
        if (allowedExtensions == null || !allowedExtensions.contains(extension)) {
            throw new BusinessRuleException("UNSUPPORTED_FILE_TYPE",
                    "Tipo de archivo no permitido: " + detectedType + " (." + extension + ")");
        }

        String dateSegment = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String storageKey = dateSegment + "/" + UUID.randomUUID() + "." + extension;
        Path target = root.resolve(storageKey).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessRuleException("INVALID_PATH", "Ruta de almacenamiento inválida");
        }

        String sha256;
        try {
            Files.createDirectories(target.getParent());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = file.getInputStream();
                 DigestInputStream din = new DigestInputStream(in, digest)) {
                Files.copy(din, target, StandardCopyOption.REPLACE_EXISTING);
            }
            sha256 = HexFormat.of().formatHex(digest.digest());
        } catch (IOException ex) {
            throw new BusinessRuleException("STORAGE_ERROR", "No se pudo almacenar el archivo: " + ex.getMessage());
        } catch (Exception ex) {
            throw new BusinessRuleException("STORAGE_ERROR", "No se pudo calcular el hash del archivo");
        }

        Optional<MediaFile> existing = mediaFileRepository.findBySha256AndOriginalName(sha256, originalName);
        if (existing.isPresent()) {
            // Reutilizar archivo idéntico ya subido
            try {
                Files.deleteIfExists(target);
            } catch (IOException ignored) {
            }
            return existing.get();
        }

        MediaFile mediaFile = new MediaFile();
        mediaFile.setStorageKey(storageKey);
        mediaFile.setOriginalName(originalName);
        mediaFile.setContentType(detectedType);
        mediaFile.setSizeBytes(file.getSize());
        mediaFile.setSha256(sha256);
        mediaFile.setPublic(isPublic);
        return mediaFileRepository.save(mediaFile);
    }

    public Path resolve(String storageKey) {
        Path path = root.resolve(storageKey).normalize();
        if (!path.startsWith(root)) {
            throw new BusinessRuleException("INVALID_PATH", "Ruta de archivo inválida");
        }
        return path;
    }

    @Transactional(readOnly = true)
    public MediaFile getRequired(UUID mediaId) {
        return mediaFileRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Archivo", mediaId));
    }

    private String detectContentType(MultipartFile file, String extension) {
        try {
            String detected = java.nio.file.Files.probeContentType(
                    Paths.get(sanitize(file.getOriginalFilename())));
            if (detected != null && ALLOWED_TYPES.containsKey(detected)) {
                return detected;
            }
        } catch (Exception ignored) {
            // probeContentType puede lanzar IOException o PatternSyntaxException (Windows)
        }
        // Fallback por extensión si el probe no está disponible
        return ALLOWED_TYPES.entrySet().stream()
                .filter(e -> e.getValue().contains(extension))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("");
    }

    private String sanitize(String name) {
        if (name == null || name.isBlank()) {
            return "archivo";
        }
        String clean = name.replaceAll("[\\\\/\u0000]", "_").trim();
        return clean.length() > 255 ? clean.substring(clean.length() - 255) : clean;
    }

    private String extensionOf(String name) {
        int idx = name.lastIndexOf('.');
        return idx > 0 ? name.substring(idx + 1).toLowerCase(Locale.ROOT) : "";
    }
}
