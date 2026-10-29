package ec.com.megalujos.pos.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.com.megalujos.catalog.domain.Product;
import ec.com.megalujos.catalog.domain.ProductVariant;
import ec.com.megalujos.catalog.domain.ProductVariantRepository;
import ec.com.megalujos.cash.domain.CashMovement;
import ec.com.megalujos.cash.domain.CashMovementRepository;
import ec.com.megalujos.cash.domain.CashSession;
import ec.com.megalujos.cash.domain.CashSessionRepository;
import ec.com.megalujos.common.exception.BusinessRuleException;
import ec.com.megalujos.common.tax.TaxRule;
import ec.com.megalujos.common.tax.TaxRuleRepository;
import ec.com.megalujos.common.tax.TaxService;
import ec.com.megalujos.inventory.application.StockService;
import ec.com.megalujos.invoicing.domain.OutboxEventRepository;
import ec.com.megalujos.pos.domain.Sale;
import ec.com.megalujos.pos.domain.SaleRepository;
import ec.com.megalujos.pos.dto.ConfirmSaleRequest;
import ec.com.megalujos.pos.dto.SaleResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios de SaleService: idempotencia, validación de pago, impuestos y caja.
 */
@ExtendWith(MockitoExtension.class)
class SaleServiceTest {

    @Mock
    private SaleRepository saleRepository;
    @Mock
    private ProductVariantRepository variantRepository;
    @Mock
    private StockService stockService;
    @Mock
    private CashSessionRepository cashSessionRepository;
    @Mock
    private CashMovementRepository cashMovementRepository;
    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Mock
    private TaxRuleRepository taxRuleRepository;

    private SaleService saleService;

    private final UUID idempotencyKey = UUID.randomUUID();
    private final UUID currentUserId = UUID.randomUUID();
    private final UUID cashSessionId = UUID.randomUUID();
    private final UUID variantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        // TaxService real con repositorio mockeado: IVA 15 resuelto en "BD"
        saleService = new SaleService(saleRepository, variantRepository, stockService,
                cashSessionRepository, cashMovementRepository, outboxEventRepository,
                new TaxService(taxRuleRepository), new ObjectMapper());
    }

    private TaxRule iva15() {
        TaxRule rule = new TaxRule();
        rule.setCode("IVA_15");
        rule.setRate(new BigDecimal("0.15"));
        rule.setActive(true);
        rule.setValidFrom(LocalDate.of(2024, 1, 1));
        return rule;
    }

    private CashSession session(String status) {
        CashSession session = new CashSession();
        session.setId(cashSessionId);
        session.setStatus(status);
        session.setOpeningAmount(new BigDecimal("50.00"));
        return session;
    }

    private ProductVariant variant(BigDecimal salePrice, boolean trackStock) {
        Product product = new Product();
        product.setName("Llanta Deportiva");
        product.setActive(true);
        product.setTrackStock(trackStock);
        product.setProductType("PHYSICAL");

        ProductVariant variant = new ProductVariant();
        variant.setId(variantId);
        variant.setProduct(product);
        variant.setSku("SKU-001");
        variant.setSalePrice(salePrice);
        variant.setActive(true);
        return variant;
    }

    private ConfirmSaleRequest request(BigDecimal quantity, BigDecimal discount, String method, BigDecimal amount) {
        ConfirmSaleRequest.SaleItemRequest item = new ConfirmSaleRequest.SaleItemRequest();
        item.setVariantId(variantId);
        item.setQuantity(quantity);
        item.setDiscount(discount);

        ConfirmSaleRequest.SalePaymentRequest payment = new ConfirmSaleRequest.SalePaymentRequest();
        payment.setMethod(method);
        payment.setAmount(amount);

        ConfirmSaleRequest req = new ConfirmSaleRequest();
        req.setCashSessionId(cashSessionId);
        req.setItems(List.of(item));
        req.setPayments(List.of(payment));
        return req;
    }

    @Test
    void confirmSale_idempotencia_devuelveVentaExistenteSinProcesarNada() {
        // Given: la venta con esa llave ya fue procesada
        Sale existing = new Sale();
        existing.setId(UUID.randomUUID());
        existing.setSaleNumber("VTA-20250101-ABCDEF");
        existing.setGrandTotal(new BigDecimal("230.00"));
        existing.setIdempotencyKey(idempotencyKey);
        when(saleRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existing));

        // When
        SaleResponseDto dto = saleService.confirmSale(request(BigDecimal.TEN, BigDecimal.ZERO, "CASH", BigDecimal.TEN),
                idempotencyKey, currentUserId);

        // Then: devuelve la venta previa sin tocar nada más
        assertEquals(existing.getId(), dto.getSaleId());
        assertEquals("ORIGINAL_IDEMPOTENT_RESPONSE", dto.getInvoiceStatus());
        verify(saleRepository, never()).save(any());
        verifyNoInteractions(cashSessionRepository, variantRepository, stockService,
                cashMovementRepository, outboxEventRepository, taxRuleRepository);
    }

    @Test
    void confirmSale_cajaCerrada_lanzaCashSessionClosed() {
        // Given: sin venta previa y sesión cerrada
        when(saleRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(cashSessionRepository.findById(cashSessionId)).thenReturn(Optional.of(session("CLOSED")));

        // When / Then
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                saleService.confirmSale(request(BigDecimal.ONE, BigDecimal.ZERO, "CASH", BigDecimal.TEN),
                        idempotencyKey, currentUserId));
        assertEquals("CASH_SESSION_CLOSED", ex.getCode());
        verify(saleRepository, never()).save(any());
    }

    @Test
    void confirmSale_pagoInsuficiente_lanzaInsufficientPayment() {
        // Given: 2 unidades a 100.00 + IVA 15% => total 230.00; solo paga 100
        when(saleRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(cashSessionRepository.findById(cashSessionId)).thenReturn(Optional.of(session("OPEN")));
        when(variantRepository.findById(variantId)).thenReturn(Optional.of(variant(new BigDecimal("100.00"), false)));
        when(taxRuleRepository.findDefaultValid(any(LocalDate.class))).thenReturn(Optional.of(iva15()));

        // When / Then
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                saleService.confirmSale(request(new BigDecimal("2"), BigDecimal.ZERO, "CASH", new BigDecimal("100.00")),
                        idempotencyKey, currentUserId));
        assertEquals("INSUFFICIENT_PAYMENT", ex.getCode());
        verify(saleRepository, never()).save(any());
    }

    @Test
    void confirmSale_pagoExacto_calculaImpuestosYRegistraMovimientoDeCaja() {
        // Given: precio 100.00 x 2 + IVA 15% => subtotal 200.00, IVA 30.00, total 230.00
        when(saleRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(cashSessionRepository.findById(cashSessionId)).thenReturn(Optional.of(session("OPEN")));
        when(variantRepository.findById(variantId)).thenReturn(Optional.of(variant(new BigDecimal("100.00"), true)));
        when(taxRuleRepository.findDefaultValid(any(LocalDate.class))).thenReturn(Optional.of(iva15()));
        when(saleRepository.save(any(Sale.class))).thenAnswer(inv -> inv.getArgument(0));

        // When: pago exacto en efectivo
        SaleResponseDto dto = saleService.confirmSale(request(new BigDecimal("2"), BigDecimal.ZERO, "CASH", new BigDecimal("230.00")),
                idempotencyKey, currentUserId);

        // Then
        assertEquals(0, new BigDecimal("200.00").compareTo(dto.getSubtotal()));
        assertEquals(0, new BigDecimal("30.00").compareTo(dto.getTaxTotal()));
        assertEquals(0, new BigDecimal("230.00").compareTo(dto.getGrandTotal()));
        assertEquals("NOT_REQUESTED", dto.getInvoiceStatus());
        assertEquals("CONFIRMED", dto.getStatus());

        // Impuesto resuelto con la regla de BD (nunca hardcodeado en el servicio)
        assertEquals(1, dto.getItems().size());
        assertEquals(0, new BigDecimal("30.00").compareTo(dto.getItems().get(0).getTaxAmount()));
        assertEquals(0, new BigDecimal("230.00").compareTo(dto.getItems().get(0).getLineTotal()));

        // Stock descontado y movimiento de caja registrado por la porción en efectivo
        verify(stockService).deductStock(eq(variantId), isNull(), eq(new BigDecimal("2")),
                eq("SALE"), eq("POS_SALE"), any(), isNull(), eq(currentUserId), any());
        ArgumentCaptor<CashMovement> captor = ArgumentCaptor.forClass(CashMovement.class);
        verify(cashMovementRepository).save(captor.capture());
        assertEquals(0, new BigDecimal("230.00").compareTo(captor.getValue().getAmount()));
        assertEquals("SALE_PAYMENT", captor.getValue().getMovementType());
        assertEquals(cashSessionId, captor.getValue().getSessionId());
    }

    @Test
    void confirmSale_descuentoExcesivoSeClampaACero() {
        // Given: descuento mayor al total de la línea => base se clampa a 0
        when(saleRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(cashSessionRepository.findById(cashSessionId)).thenReturn(Optional.of(session("OPEN")));
        when(variantRepository.findById(variantId)).thenReturn(Optional.of(variant(new BigDecimal("10.00"), false)));
        when(taxRuleRepository.findDefaultValid(any(LocalDate.class))).thenReturn(Optional.of(iva15()));
        when(saleRepository.save(any(Sale.class))).thenAnswer(inv -> inv.getArgument(0));

        // When: 1 x 10.00 con descuento 1000 => línea 0, total 0
        SaleResponseDto dto = saleService.confirmSale(request(BigDecimal.ONE, new BigDecimal("1000.00"), "CASH", new BigDecimal("0.01")),
                idempotencyKey, currentUserId);

        // Then
        assertEquals(0, BigDecimal.ZERO.compareTo(dto.getSubtotal()), "La línea negativa se clampa a cero");
        assertEquals(0, BigDecimal.ZERO.compareTo(dto.getTaxTotal()));
        assertEquals(0, BigDecimal.ZERO.compareTo(dto.getGrandTotal()));
        verify(stockService, never()).deductStock(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void confirmSale_descuentoNegativo_lanzaInvalidDiscount() {
        // Given: un descuento negativo ya no está permitido
        when(saleRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(cashSessionRepository.findById(cashSessionId)).thenReturn(Optional.of(session("OPEN")));
        when(variantRepository.findById(variantId)).thenReturn(Optional.of(variant(new BigDecimal("100.00"), false)));

        // When / Then
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                saleService.confirmSale(request(BigDecimal.ONE, new BigDecimal("-50.00"), "CASH", new BigDecimal("172.50")),
                        idempotencyKey, currentUserId));
        assertEquals("INVALID_DISCOUNT", ex.getCode());
    }

    @Test
    void confirmSale_pagoCero_lanzaInvalidPayment() {
        // Given
        when(saleRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(cashSessionRepository.findById(cashSessionId)).thenReturn(Optional.of(session("OPEN")));
        when(variantRepository.findById(variantId)).thenReturn(Optional.of(variant(new BigDecimal("10.00"), false)));
        when(taxRuleRepository.findDefaultValid(any(LocalDate.class))).thenReturn(Optional.of(iva15()));

        // When / Then
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                saleService.confirmSale(request(BigDecimal.ONE, BigDecimal.ZERO, "CASH", BigDecimal.ZERO),
                        idempotencyKey, currentUserId));
        assertEquals("INVALID_PAYMENT", ex.getCode());
    }

    @Test
    void confirmSale_productoInactivo_lanzaInactiveProduct() {
        // Given
        when(saleRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(cashSessionRepository.findById(cashSessionId)).thenReturn(Optional.of(session("OPEN")));
        ProductVariant inactiva = variant(new BigDecimal("10.00"), false);
        inactiva.setActive(false);
        when(variantRepository.findById(variantId)).thenReturn(Optional.of(inactiva));

        // When / Then
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                saleService.confirmSale(request(BigDecimal.ONE, BigDecimal.ZERO, "CASH", BigDecimal.TEN),
                        idempotencyKey, currentUserId));
        assertEquals("INACTIVE_PRODUCT", ex.getCode());
    }
}
