package ec.com.megalujos.services.application;

import ec.com.megalujos.cash.domain.CashMovementRepository;
import ec.com.megalujos.cash.domain.CashSessionRepository;
import ec.com.megalujos.catalog.domain.MediaFileRepository;
import ec.com.megalujos.common.audit.AuditService;
import ec.com.megalujos.common.exception.BusinessRuleException;
import ec.com.megalujos.common.tax.TaxService;
import ec.com.megalujos.inventory.application.StockService;
import ec.com.megalujos.pos.application.SaleChargeService;
import ec.com.megalujos.services.domain.*;
import ec.com.megalujos.services.dto.ChangeStatusRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios de ServiceOrderService: transiciones de estado de la orden.
 * Flujo permitido: QUOTED -> CONFIRMED -> IN_PROGRESS -> READY_FOR_DELIVERY -> DELIVERED;
 * CANCELLED y DELIVERED son estados terminales.
 */
@ExtendWith(MockitoExtension.class)
class ServiceOrderServiceTest {

    @Mock
    private ServiceOrderRepository orderRepository;
    @Mock
    private ServiceMaterialRepository materialRepository;
    @Mock
    private ServiceAttachmentRepository attachmentRepository;
    @Mock
    private ServiceAdvanceRepository advanceRepository;
    @Mock
    private MediaFileRepository mediaFileRepository;
    @Mock
    private CashSessionRepository cashSessionRepository;
    @Mock
    private CashMovementRepository cashMovementRepository;
    @Mock
    private StockService stockService;
    @Mock
    private TaxService taxService;
    @Mock
    private SaleChargeService saleChargeService;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private ServiceOrderService serviceOrderService;

    private final UUID orderId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();

    private ServiceOrder order(String status) {
        ServiceOrder order = new ServiceOrder();
        order.setId(orderId);
        order.setOrderNumber("OTS-20250101-ABCD");
        order.setStatus(status);
        return order;
    }

    private ChangeStatusRequest changeStatus(String status) {
        ChangeStatusRequest request = new ChangeStatusRequest();
        request.setStatus(status);
        request.setNotes("Cambio de estado");
        return request;
    }

    @Test
    void changeStatus_deliveredANoPuedeVolverAInProgress() {
        // Given: DELIVERED es estado terminal
        ServiceOrder order = order("DELIVERED");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When / Then
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                serviceOrderService.changeStatus(orderId, changeStatus("IN_PROGRESS"), actorId, "10.0.0.1"));
        assertEquals("INVALID_STATUS_TRANSITION", ex.getCode());
        assertEquals("DELIVERED", order.getStatus(), "El estado no debe cambiarse");
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void changeStatus_confirmedAInProgress_esValidoYRegistraHistorial() {
        // Given
        ServiceOrder order = order("CONFIRMED");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(ServiceOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(attachmentRepository.findByServiceOrderId(orderId)).thenReturn(List.of());

        // When
        serviceOrderService.changeStatus(orderId, changeStatus("IN_PROGRESS"), actorId, "10.0.0.1");

        // Then
        assertEquals("IN_PROGRESS", order.getStatus());
        assertEquals(1, order.getStatusHistory().size());
        ServiceStatusHistory history = order.getStatusHistory().get(0);
        assertEquals("CONFIRMED", history.getOldStatus());
        assertEquals("IN_PROGRESS", history.getNewStatus());
        assertEquals(actorId, history.getActorId());
        verify(auditService).record(eq(actorId), eq("SERVICE_STATUS_CHANGED"), eq("SERVICE_ORDER"),
                eq(orderId), any(), eq("10.0.0.1"));
    }

    @Test
    void changeStatus_readyForDeliveryADelivered_marcaCompletedAt() {
        // Given
        ServiceOrder order = order("READY_FOR_DELIVERY");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(ServiceOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(attachmentRepository.findByServiceOrderId(orderId)).thenReturn(List.of());

        // When
        serviceOrderService.changeStatus(orderId, changeStatus("DELIVERED"), actorId, "10.0.0.1");

        // Then
        assertEquals("DELIVERED", order.getStatus());
        assertNotNull(order.getCompletedAt(), "DELIVERED debe fijar completedAt");
    }

    @Test
    void changeStatus_quotedACancelled_esValido() {
        // Given
        ServiceOrder order = order("QUOTED");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(ServiceOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(attachmentRepository.findByServiceOrderId(orderId)).thenReturn(List.of());

        // When
        serviceOrderService.changeStatus(orderId, changeStatus("CANCELLED"), actorId, "10.0.0.1");

        // Then
        assertEquals("CANCELLED", order.getStatus());
    }

    @Test
    void changeStatus_cancelledNoAdmiteNuevaTransicion() {
        // Given
        ServiceOrder order = order("CANCELLED");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When / Then
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                serviceOrderService.changeStatus(orderId, changeStatus("CONFIRMED"), actorId, "10.0.0.1"));
        assertEquals("INVALID_STATUS_TRANSITION", ex.getCode());
    }

    @Test
    void changeStatus_quotedASinTransicionAInProgress_lanzaInvalid() {
        // Given: QUOTED solo puede ir a CONFIRMED o CANCELLED
        ServiceOrder order = order("QUOTED");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When / Then
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                serviceOrderService.changeStatus(orderId, changeStatus("IN_PROGRESS"), actorId, "10.0.0.1"));
        assertEquals("INVALID_STATUS_TRANSITION", ex.getCode());
    }

    @Test
    void changeStatus_transicionNormalQuotedAConfirmed() {
        // Given
        ServiceOrder order = order("QUOTED");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(ServiceOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(attachmentRepository.findByServiceOrderId(orderId)).thenReturn(List.of());

        // When
        serviceOrderService.changeStatus(orderId, changeStatus("CONFIRMED"), actorId, "10.0.0.1");

        // Then
        assertEquals("CONFIRMED", order.getStatus());
        assertNull(order.getCompletedAt());
    }

    @Test
    void changeStatus_ordenInexistente_lanzaResourceNotFound() {
        // Given
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // When / Then
        assertThrows(ec.com.megalujos.common.exception.ResourceNotFoundException.class, () ->
                serviceOrderService.changeStatus(orderId, changeStatus("IN_PROGRESS"), actorId, "10.0.0.1"));
    }
}
