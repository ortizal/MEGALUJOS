package ec.com.megalujos.common.files;

import ec.com.megalujos.catalog.domain.MediaFile;
import ec.com.megalujos.catalog.domain.MediaFileRepository;
import ec.com.megalujos.common.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios de FileStorageService: validación de tipo, tamaño y almacenamiento.
 * Files.probeContentType puede devolver null en un tempfile; el fallback por
 * extensión debe aceptar archivos válidos igualmente.
 */
@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @Mock
    private MediaFileRepository mediaFileRepository;

    @TempDir
    Path tempDir;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService(mediaFileRepository, tempDir.toString());
    }

    @Test
    void store_archivoVacio_lanzaEmptyFile() {
        // Given
        MockMultipartFile vacio = new MockMultipartFile("file", "vacio.jpg", "image/jpeg", new byte[0]);

        // When / Then
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> fileStorageService.store(vacio, false));
        assertEquals("EMPTY_FILE", ex.getCode());
        verifyNoInteractions(mediaFileRepository);
    }

    @Test
    void store_archivoNulo_lanzaEmptyFile() {
        // When / Then
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> fileStorageService.store(null, false));
        assertEquals("EMPTY_FILE", ex.getCode());
    }

    @Test
    void store_archivoExe_lanzaUnsupportedFileType() {
        // Given: ejecutable con MIME genérico
        MockMultipartFile exe = new MockMultipartFile("file", "instalador.exe",
                "application/octet-stream", new byte[]{0x4D, 0x5A, 0x00, 0x01});

        // When / Then
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> fileStorageService.store(exe, false));
        assertEquals("UNSUPPORTED_FILE_TYPE", ex.getCode());
        verifyNoInteractions(mediaFileRepository);
    }

    @Test
    void store_archivoJpg_aceptadoPorProbeOFallbackDeExtension() {
        // Given: contenido JPEG mínimo; el probe del SO puede fallar en el tempfile
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46};
        MockMultipartFile foto = new MockMultipartFile("file", "foto.jpg", "image/jpeg", jpeg);
        when(mediaFileRepository.findBySha256AndOriginalName(anyString(), anyString())).thenReturn(Optional.empty());
        when(mediaFileRepository.save(any(MediaFile.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        MediaFile stored = fileStorageService.store(foto, false);

        // Then
        assertNotNull(stored);
        assertEquals("image/jpeg", stored.getContentType());
        assertEquals("foto.jpg", stored.getOriginalName());
        assertEquals(jpeg.length, stored.getSizeBytes());
        assertNotNull(stored.getSha256());
        assertEquals(64, stored.getSha256().length(), "SHA-256 debe tener 64 hex");
        assertTrue(stored.getStorageKey().endsWith(".jpg"));
        assertFalse(stored.isPublic());
        verify(mediaFileRepository).save(any(MediaFile.class));

        // El archivo quedó materializado bajo el root configurado
        Path destino = tempDir.resolve(stored.getStorageKey());
        assertTrue(Files.exists(destino), "El archivo debe existir en " + destino);
    }

    @Test
    void store_archivoIdempotente_reutilizaElExistente() {
        // Given: mismo hash y nombre => reutiliza el MediaFile previo
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
        MockMultipartFile foto = new MockMultipartFile("file", "foto.jpg", "image/jpeg", jpeg);
        MediaFile existente = new MediaFile();
        existente.setId(java.util.UUID.randomUUID());
        existente.setOriginalName("foto.jpg");
        when(mediaFileRepository.findBySha256AndOriginalName(anyString(), anyString()))
                .thenReturn(Optional.of(existente));

        // When
        MediaFile stored = fileStorageService.store(foto, true);

        // Then
        assertSame(existente, stored);
        verify(mediaFileRepository, never()).save(any());
    }

    @Test
    void store_pngAceptado() {
        // Given
        byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        MockMultipartFile logo = new MockMultipartFile("file", "logo.png", "image/png", png);
        when(mediaFileRepository.findBySha256AndOriginalName(anyString(), anyString())).thenReturn(Optional.empty());
        when(mediaFileRepository.save(any(MediaFile.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        MediaFile stored = fileStorageService.store(logo, true);

        // Then
        assertEquals("image/png", stored.getContentType());
        assertTrue(stored.isPublic());
    }

    @Test
    void resolve_conPathTraversal_lanzaInvalidPath() {
        // When / Then: intento de escapar del root
        assertThrows(BusinessRuleException.class, () -> fileStorageService.resolve("../../etc/passwd"));
    }

    @Test
    void resolve_rutaValida_devuelvePathBajoElRoot() {
        // When
        Path path = fileStorageService.resolve("2025/01/archivo.jpg");

        // Then
        assertTrue(path.startsWith(tempDir.toAbsolutePath()));
    }
}
