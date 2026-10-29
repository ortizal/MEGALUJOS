package ec.com.megalujos.cash.application;

import ec.com.megalujos.cash.domain.*;
import ec.com.megalujos.common.audit.AuditService;
import ec.com.megalujos.common.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios de CashSessionService: apertura y cierre de sesión de caja.
 */
@ExtendWith(MockitoExtension.class)
class CashSessionServiceTest {

    @Mock
    private CashSessionRepository sessionRepository;
    @Mock
    private CashSessionSearchRepository sessionSearchRepository;
    @Mock
    private CashRegisterRepository registerRepository;
    @Mock
    private CashMovementRepository movementRepository;
    @Mock
    private CashCountRepository countRepository;
    @Mock
    private CashReconciliationRepository reconciliationRepository;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private CashSessionService cashSessionService;

    private final UUID registerId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    private CashRegister register() {
        CashRegister register = new CashRegister();
        register.setId(registerId);
        register.setName("Caja 1");
        register.setActive(true);
        return register;
    }

    @Test
    void openSession_conSesionYaAbierta_lanzaSessionAlreadyOpen() {
        // Given: ya hay una sesión OPEN en la registradora
        when(registerRepository.findById(registerId)).thenReturn(Optional.of(register()));
        when(sessionRepository.findByRegisterIdAndStatus(registerId, "OPEN"))
                .thenReturn(Optional.of(new CashSession()));

        // When / Then
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                cashSessionService.openSession(registerId, new BigDecimal("50.00"), userId, null, "10.0.0.1"));
        assertEquals("SESSION_ALREADY_OPEN", ex.getCode());
        verify(sessionRepository, never()).save(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void openSession_creaSesionOpenConFondoPorDefecto() {
        // Given
        when(registerRepository.findById(registerId)).thenReturn(Optional.of(register()));
        when(sessionRepository.findByRegisterIdAndStatus(registerId, "OPEN")).thenReturn(Optional.empty());
        when(sessionRepository.save(any(CashSession.class))).thenAnswer(inv -> inv.getArgument(0));

        // When: openingAmount null => fondo en cero
        CashSession saved = cashSessionService.openSession(registerId, null, userId, "Apertura", "10.0.0.1");

        // Then
        assertEquals("OPEN", saved.getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(saved.getOpeningAmount()));
        assertEquals(userId, saved.getOpenedBy());
        assertNotNull(saved.getOpenedAt());
        verify(auditService).record(eq(userId), eq("CASH_OPENED"), eq("CASH_SESSION"), any(), any(), eq("10.0.0.1"));
    }

    @Test
    void openSession_registradorInexistente_lanzaResourceNotFound() {
        // Given
        when(registerRepository.findById(registerId)).thenReturn(Optional.empty());

        // When / Then
        assertThrows(ec.com.megalujos.common.exception.ResourceNotFoundException.class, () ->
                cashSessionService.openSession(registerId, BigDecimal.TEN, userId, null, "10.0.0.1"));
    }

    @Test
    void closeSession_calculaExpectedYDifference() {
        // Given: fondo 50 + ingresos 200 - egresos 30 => esperado 220; contado 210 => diferencia -10
        CashSession session = new CashSession();
        session.setId(sessionId);
        session.setStatus("OPEN");
        session.setOpeningAmount(new BigDecimal("50.00"));
        session.setNotes("Apertura");
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(movementRepository.sumIncomeBySessionId(sessionId)).thenReturn(new BigDecimal("200.00"));
        when(movementRepository.sumExpenseBySessionId(sessionId)).thenReturn(new BigDecimal("30.00"));
        when(sessionRepository.save(any(CashSession.class))).thenAnswer(inv -> inv.getArgument(0));

        // When: sin arqueo por medio de pago (counts null)
        CashSession saved = cashSessionService.closeSession(sessionId, new BigDecimal("210.00"), null, userId, "Cierre", "10.0.0.1");

        // Then
        assertEquals("CLOSED", saved.getStatus());
        assertEquals(0, new BigDecimal("220.00").compareTo(saved.getExpectedAmount()));
        assertEquals(0, new BigDecimal("-10.00").compareTo(saved.getDifferenceAmount()));
        assertEquals(0, new BigDecimal("210.00").compareTo(saved.getCountedAmount()));
        assertEquals(userId, saved.getClosedBy());
        assertNotNull(saved.getClosedAt());
        assertTrue(saved.getNotes().contains("Cierre"));
        verify(auditService).record(eq(userId), eq("CASH_CLOSED"), eq("CASH_SESSION"), eq(sessionId), any(), eq("10.0.0.1"));
    }

    @Test
    void closeSession_sinDiferencia_cuandoElContadoCoincideConElEsperado() {
        // Given
        CashSession session = new CashSession();
        session.setId(sessionId);
        session.setStatus("OPEN");
        session.setOpeningAmount(new BigDecimal("50.00"));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(movementRepository.sumIncomeBySessionId(sessionId)).thenReturn(new BigDecimal("200.00"));
        when(movementRepository.sumExpenseBySessionId(sessionId)).thenReturn(new BigDecimal("30.00"));
        when(sessionRepository.save(any(CashSession.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        CashSession saved = cashSessionService.closeSession(sessionId, new BigDecimal("220.00"), null, userId, null, "10.0.0.1");

        // Then
        assertEquals(0, BigDecimal.ZERO.compareTo(saved.getDifferenceAmount()));
    }

    @Test
    void closeSession_sesionNoAbierta_lanzaSessionNotOpen() {
        // Given
        CashSession session = new CashSession();
        session.setId(sessionId);
        session.setStatus("CLOSED");
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        // When / Then
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                cashSessionService.closeSession(sessionId, BigDecimal.TEN, null, userId, null, "10.0.0.1"));
        assertEquals("SESSION_NOT_OPEN", ex.getCode());
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void recordMovement_sesionCerrada_lanzaSessionClosed() {
        // Given
        CashSession session = new CashSession();
        session.setId(sessionId);
        session.setStatus("CLOSED");
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        // When / Then
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                cashSessionService.recordMovement(sessionId, "EXPENSE", BigDecimal.TEN, "Retiro", userId));
        assertEquals("SESSION_CLOSED", ex.getCode());
    }

    @Test
    void getActiveSession_devuelveLaSesionOpenDeLaCaja() {
        // Given
        CashSession abierta = new CashSession();
        abierta.setId(sessionId);
        when(sessionRepository.findByRegisterIdAndStatus(registerId, "OPEN")).thenReturn(Optional.of(abierta));

        // When / Then
        assertEquals(abierta, cashSessionService.getActiveSession(registerId));
    }

    @Test
    void getActiveSession_sinSesionDevuelveNull() {
        // Given
        when(sessionRepository.findFirstByStatusOrderByOpenedAtDesc("OPEN")).thenReturn(Optional.empty());

        // When / Then
        assertNull(cashSessionService.getActiveSession(null));
    }
}
