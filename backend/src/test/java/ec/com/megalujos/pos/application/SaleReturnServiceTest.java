package ec.com.megalujos.pos.application;

import ec.com.megalujos.cash.domain.CashMovement;
import ec.com.megalujos.cash.domain.CashMovementRepository;
import ec.com.megalujos.common.audit.AuditService;
import ec.com.megalujos.common.exception.BusinessRuleException;
import ec.com.megalujos.inventory.application.StockService;
import ec.com.megalujos.pos.domain.*;
import ec.com.megalujos.pos.dto.CreateReturnRequest;
import ec.com.megalujos.pos.dto.SaleReturnDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios de SaleReturnService: validación de cantidades devueltas
 * y cálculo proporcional del monto de devolución.
 */
@ExtendWith(MockitoExtension.class)
class SaleReturnServiceTest {

    @Mock
    private SaleRepository saleRepository;
    @Mock
    private SaleSearchRepository saleSearchRepository;
    @Mock
    private SaleReturnRepository saleReturnRepository;
    @Mock
    private SaleReturnItemRepository saleReturnItemRepository;
    @Mock
    private CashMovementRepository cashMovementRepository;
    @Mock
    private ec.com.megalujos.catalog.domain.ProductVariantRepository variantRepository;
    @Mock
    private StockService stockService;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private SaleReturnService saleReturnService;

    private final UUID saleId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();
    private final UUID saleItemId = UUID.randomUUID();
    private final UUID variantId = UUID.randomUUID();

    /**
     * Venta CONFIRMED con un ítem: 2 unidades a 100.00, descuento 10.00, IVA 15%.
     */
    private Sale ventaConfirmada() {
        Sale sale = new Sale();
        sale.setId(saleId);
        sale.setSaleNumber("VTA-20250101-ABCDEF");
        sale.setStatus("CONFIRMED");
        sale.setCashSessionId(UUID.randomUUID());
        sale.setGrandTotal(new BigDecimal("250.00"));
        sale.setNotes(null);

        SaleItem item = new SaleItem();
        item.setId(saleItemId);
        item.setSale(sale);
        item.setVariantId(variantId);
        item.setDescription("Llanta Deportiva - SKU-001");
        item.setQuantity(new BigDecimal("2"));
        item.setUnitPrice(new BigDecimal("100.00"));
        item.setDiscount(new BigDecimal("10.00"));
        item.setTaxRate(new BigDecimal("0.15"));
        item.setLineTotal(new BigDecimal("230.00"));
        sale.getItems().add(item);
        return sale;
    }

    private CreateReturnRequest.ReturnItemRequest itemRequest(BigDecimal quantity) {
        CreateReturnRequest.ReturnItemRequest item = new CreateReturnRequest.ReturnItemRequest();
        item.setSaleItemId(saleItemId);
        item.setQuantity(quantity);
        item.setRestock(true);
        return item;
    }

    private CreateReturnRequest request(List<CreateReturnRequest.ReturnItemRequest> items) {
        CreateReturnRequest req = new CreateReturnRequest();
        req.setReason("Producto defectuoso");
        req.setItems(items);
        return req;
    }

    @Test
    void createReturn_devolverMasDeLoVendido_lanzaQuantityExceedsReturnable() {
        // Given: ítem vendió 2, ya se devolvió 1 => máximo devolvible 1; piden 2
        Sale sale = ventaConfirmada();
        when(saleRepository.findById(saleId)).thenReturn(Optional.of(sale));
        when(saleReturnItemRepository.sumReturnedQuantityBySaleItem(saleItemId)).thenReturn(BigDecimal.ONE);

        // When / Then
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                saleReturnService.createReturn(saleId, request(List.of(itemRequest(new BigDecimal("2")))), actorId, "10.0.0.1"));
        assertEquals("QUANTITY_EXCEEDS_RETURNABLE", ex.getCode());
        verify(saleReturnRepository, never()).save(any());
    }

    @Test
    void createReturn_dosLineasMismoItem_noExcedenElMaximoEnConjunto() {
        // Given: vendió 2, sin devoluciones previas; dos líneas de 1.5 cada una => la segunda excede
        Sale sale = ventaConfirmada();
        when(saleRepository.findById(saleId)).thenReturn(Optional.of(sale));
        when(saleReturnItemRepository.sumReturnedQuantityBySaleItem(saleItemId)).thenReturn(BigDecimal.ZERO);

        // When / Then
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                saleReturnService.createReturn(saleId, request(List.of(
                        itemRequest(new BigDecimal("1.5")), itemRequest(new BigDecimal("1.5")))), actorId, "10.0.0.1"));
        assertEquals("QUANTITY_EXCEEDS_RETURNABLE", ex.getCode());
    }

    @Test
    void createReturn_calculaProporcionalmenteElReturnAmount() {
        // Given
        Sale sale = ventaConfirmada();
        when(saleRepository.findById(saleId)).thenReturn(Optional.of(sale));
        when(saleReturnItemRepository.sumReturnedQuantityBySaleItem(saleItemId)).thenReturn(BigDecimal.ZERO);
        when(saleReturnRepository.save(any(SaleReturn.class))).thenAnswer(inv -> inv.getArgument(0));
        // Total devuelto acumulado < grandTotal => estado PARTIALLY_RETURNED
        when(saleReturnRepository.sumTotalReturnedBySaleId(saleId)).thenReturn(new BigDecimal("109.25"));
        when(saleRepository.save(any(Sale.class))).thenAnswer(inv -> inv.getArgument(0));

        // When: devolver 1 de 2 unidades
        SaleReturnDto dto = saleReturnService.createReturn(saleId, request(List.of(itemRequest(BigDecimal.ONE))), actorId, "10.0.0.1");

        // Then: cálculo a mano:
        //   base = 100.00 * 1 = 100.0000
        //   parte del descuento = 10.00 * 1 / 2 = 5.0000
        //   neto = 95.0000
        //   return_amount = 95.0000 * (1 + 0.15) = 109.25
        assertEquals(0, new BigDecimal("109.25").compareTo(dto.getTotalReturned()));
        assertEquals(1, dto.getItems().size());
        assertEquals(0, new BigDecimal("109.25").compareTo(dto.getItems().get(0).getReturnAmount()));
        assertEquals(0, BigDecimal.ONE.compareTo(dto.getItems().get(0).getQuantity()));
        assertEquals("CASH", dto.getPaymentMethod());
        assertEquals("PARTIALLY_RETURNED", sale.getStatus());

        // Reintegro a inventario y movimiento de caja por el total devuelto
        verify(stockService).addStock(eq(variantId), isNull(), eq(BigDecimal.ONE), isNull(),
                eq("SALE_RETURN"), eq("SALE_RETURN"), any(), any(), eq(actorId), any());
        ArgumentCaptor<CashMovement> captor = ArgumentCaptor.forClass(CashMovement.class);
        verify(cashMovementRepository).save(captor.capture());
        assertEquals(0, new BigDecimal("109.25").compareTo(captor.getValue().getAmount()));
        assertEquals("REFUND", captor.getValue().getMovementType());
        verify(auditService).record(eq(actorId), eq("SALE_RETURNED"), eq("SALE"), eq(saleId), any(), eq("10.0.0.1"));
    }

    @Test
    void createReturn_ventaTotal_marcaLaVentaComoReturned() {
        // Given: el acumulado devuelto cubre el grandTotal
        Sale sale = ventaConfirmada();
        when(saleRepository.findById(saleId)).thenReturn(Optional.of(sale));
        when(saleReturnItemRepository.sumReturnedQuantityBySaleItem(saleItemId)).thenReturn(BigDecimal.ZERO);
        when(saleReturnRepository.save(any(SaleReturn.class))).thenAnswer(inv -> inv.getArgument(0));
        when(saleReturnRepository.sumTotalReturnedBySaleId(saleId)).thenReturn(new BigDecimal("250.00"));
        when(saleRepository.save(any(Sale.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        saleReturnService.createReturn(saleId, request(List.of(itemRequest(BigDecimal.ONE))), actorId, "10.0.0.1");

        // Then
        assertEquals("RETURNED", sale.getStatus());
    }

    @Test
    void createReturn_ventaNoDevolvible_lanzaSaleNotReturnable() {
        // Given: ventas VOIDED no se pueden devolver
        Sale sale = ventaConfirmada();
        sale.setStatus("VOIDED");
        when(saleRepository.findById(saleId)).thenReturn(Optional.of(sale));

        // When / Then
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                saleReturnService.createReturn(saleId, request(List.of(itemRequest(BigDecimal.ONE))), actorId, "10.0.0.1"));
        assertEquals("SALE_NOT_RETURNABLE", ex.getCode());
    }

    @Test
    void createReturn_medioDePagoInvalido_lanzaInvalidPaymentMethod() {
        // Given
        Sale sale = ventaConfirmada();
        when(saleRepository.findById(saleId)).thenReturn(Optional.of(sale));
        CreateReturnRequest req = request(List.of(itemRequest(BigDecimal.ONE)));
        req.setPaymentMethod("BITCOIN");

        // When / Then
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                saleReturnService.createReturn(saleId, req, actorId, "10.0.0.1"));
        assertEquals("INVALID_PAYMENT_METHOD", ex.getCode());
    }

    @Test
    void createReturn_ventainexistente_lanzaResourceNotFound() {
        // Given
        when(saleRepository.findById(saleId)).thenReturn(Optional.empty());

        // When / Then
        assertThrows(ec.com.megalujos.common.exception.ResourceNotFoundException.class, () ->
                saleReturnService.createReturn(saleId, request(List.of(itemRequest(BigDecimal.ONE))), actorId, "10.0.0.1"));
    }

    @Test
    void createReturn_sinRestock_noReintegraInventario() {
        // Given
        Sale sale = ventaConfirmada();
        when(saleRepository.findById(saleId)).thenReturn(Optional.of(sale));
        when(saleReturnItemRepository.sumReturnedQuantityBySaleItem(saleItemId)).thenReturn(BigDecimal.ZERO);
        when(saleReturnRepository.save(any(SaleReturn.class))).thenAnswer(inv -> inv.getArgument(0));
        when(saleReturnRepository.sumTotalReturnedBySaleId(saleId)).thenReturn(new BigDecimal("109.25"));
        when(saleRepository.save(any(Sale.class))).thenAnswer(inv -> inv.getArgument(0));
        CreateReturnRequest.ReturnItemRequest sinRestock = itemRequest(BigDecimal.ONE);
        sinRestock.setRestock(false);

        // When
        saleReturnService.createReturn(saleId, request(List.of(sinRestock)), actorId, "10.0.0.1");

        // Then: no se toca el inventario
        verify(stockService, never()).addStock(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }
}
