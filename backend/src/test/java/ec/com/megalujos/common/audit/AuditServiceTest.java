package ec.com.megalujos.common.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios de AuditService: la auditoría nunca debe romper la operación principal.
 */
@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository repository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AuditService auditService;

    @Test
    void record_noLanzaExcepcionCuandoElRepositoryFalla() {
        // Given: el repositorio revienta (p. ej. caída de BD)
        when(repository.save(any(AuditLog.class))).thenThrow(new RuntimeException("BD caída"));

        // When / Then: no debe propagar la excepción
        assertDoesNotThrow(() -> auditService.record(
                UUID.randomUUID(), "VENTA_CONFIRMADA", "SALE", UUID.randomUUID(), Map.of("total", "10"), "127.0.0.1"));
    }

    @Test
    void record_guardaMetadataVaciaCuandoMetadataEsNull() throws Exception {
        // Given
        when(repository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        auditService.record(UUID.randomUUID(), "ACCION", "ENTIDAD", UUID.randomUUID(), null, "127.0.0.1");

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        assertEquals("{}", captor.getValue().getMetadata());
        assertEquals("ACCION", captor.getValue().getAction());
        verify(objectMapper, never()).writeValueAsString(any());
    }

    @Test
    void record_serializaLaMetadataComoJson() throws Exception {
        // Given
        when(objectMapper.writeValueAsString(Map.of("clave", "valor"))).thenReturn("{\"clave\":\"valor\"}");
        when(repository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        auditService.record(UUID.randomUUID(), "ACCION", "ENTIDAD", UUID.randomUUID(), Map.of("clave", "valor"), "127.0.0.1");

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        assertEquals("{\"clave\":\"valor\"}", captor.getValue().getMetadata());
    }

    @Test
    void record_noLanzaExcepcionCuandoLaSerializacionFalla() throws Exception {
        // Given: ObjectMapper revienta al serializar
        when(objectMapper.writeValueAsString(any())).thenThrow(mock(JsonProcessingException.class));

        // When / Then
        assertDoesNotThrow(() -> auditService.record(
                UUID.randomUUID(), "ACCION", "ENTIDAD", UUID.randomUUID(), new Object(), "127.0.0.1"));
        verify(repository, never()).save(any());
    }
}
