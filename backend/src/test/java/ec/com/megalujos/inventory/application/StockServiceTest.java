package ec.com.megalujos.inventory.application;

import ec.com.megalujos.common.exception.BusinessRuleException;
import ec.com.megalujos.inventory.domain.*;
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
 * Tests unitarios de StockService: descuentos, ingresos y validación de cantidades.
 */
@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private StockBalanceRepository stockBalanceRepository;
    @Mock
    private StockMovementRepository stockMovementRepository;
    @Mock
    private WarehouseRepository warehouseRepository;

    @InjectMocks
    private StockService stockService;

    private final UUID variantId = UUID.randomUUID();
    private final UUID warehouseId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();

    private StockBalance balance(BigDecimal quantity, BigDecimal reserved) {
        return new StockBalance(new StockBalanceId(variantId, warehouseId), quantity, reserved);
    }

    @Test
    void deductStock_stockInsuficiente_lanzaInsufficientStock() {
        // Given: disponible 5 (qty 5 - reserved 0), se piden 10
        when(stockBalanceRepository.findByIdForUpdate(any(StockBalanceId.class)))
                .thenReturn(Optional.of(balance(new BigDecimal("5"), BigDecimal.ZERO)));

        // When / Then
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                stockService.deductStock(variantId, warehouseId, new BigDecimal("10"),
                        "SALE", "POS_SALE", UUID.randomUUID(), null, actorId, "nota"));
        assertEquals("INSUFFICIENT_STOCK", ex.getCode());
        verify(stockBalanceRepository, never()).save(any());
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void deductStock_cantidadCeroONegativa_lanzaInvalidQuantity() {
        // When / Then: cantidad 0
        BusinessRuleException ex0 = assertThrows(BusinessRuleException.class, () ->
                stockService.deductStock(variantId, warehouseId, BigDecimal.ZERO,
                        "SALE", "POS_SALE", UUID.randomUUID(), null, actorId, null));
        assertEquals("INVALID_QUANTITY", ex0.getCode());

        // Cantidad negativa
        BusinessRuleException exNeg = assertThrows(BusinessRuleException.class, () ->
                stockService.deductStock(variantId, warehouseId, new BigDecimal("-2"),
                        "SALE", "POS_SALE", UUID.randomUUID(), null, actorId, null));
        assertEquals("INVALID_QUANTITY", exNeg.getCode());

        // No toca el repositorio para cantidades inválidas
        verifyNoInteractions(stockBalanceRepository);
    }

    @Test
    void deductStock_sinRegistroDeInventario_lanzaStockNotFound() {
        // Given
        when(stockBalanceRepository.findByIdForUpdate(any(StockBalanceId.class))).thenReturn(Optional.empty());

        // When / Then
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                stockService.deductStock(variantId, warehouseId, BigDecimal.ONE,
                        "SALE", "POS_SALE", UUID.randomUUID(), null, actorId, null));
        assertEquals("STOCK_NOT_FOUND", ex.getCode());
    }

    @Test
    void deductStock_exitoso_descuentaYRegistraMovimientoNegativo() {
        // Given: disponible 10, se descuentan 4
        StockBalance bal = balance(new BigDecimal("10"), BigDecimal.ZERO);
        when(stockBalanceRepository.findByIdForUpdate(any(StockBalanceId.class))).thenReturn(Optional.of(bal));
        when(stockBalanceRepository.save(any(StockBalance.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        StockMovement movement = stockService.deductStock(variantId, warehouseId, new BigDecimal("4"),
                "SALE", "POS_SALE", UUID.randomUUID(), null, actorId, "Venta POS");

        // Then
        assertEquals(0, new BigDecimal("6").compareTo(bal.getQuantity()), "El balance debe quedar en 6");
        assertEquals(0, new BigDecimal("-4").compareTo(movement.getQuantityDelta()));
        assertEquals(0, new BigDecimal("6").compareTo(movement.getBalanceAfter()));
        assertEquals("SALE", movement.getMovementType());
        assertEquals(variantId, movement.getVariantId());
        assertEquals(warehouseId, movement.getWarehouseId());
        assertEquals(actorId, movement.getActorId());
    }

    @Test
    void addStock_creaMovimientoConDeltaPositivoYBalanceCorrecto() {
        // Given: balance existente con 3 unidades; se ingresan 4
        StockBalance bal = balance(new BigDecimal("3"), BigDecimal.ZERO);
        when(stockBalanceRepository.findByIdForUpdate(any(StockBalanceId.class))).thenReturn(Optional.of(bal));
        when(stockBalanceRepository.save(any(StockBalance.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(inv -> inv.getArgument(0));
        UUID sourceId = UUID.randomUUID();

        // When
        StockMovement movement = stockService.addStock(variantId, warehouseId, new BigDecimal("4"),
                new BigDecimal("2.5000"), "PURCHASE", "GOODS_RECEIPT", sourceId, null, actorId, "Recepción");

        // Then
        ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(captor.capture());
        StockMovement saved = captor.getValue();
        assertEquals(0, new BigDecimal("4").compareTo(saved.getQuantityDelta()), "Delta positivo");
        assertEquals(0, new BigDecimal("7").compareTo(saved.getBalanceAfter()), "Balance after = 3 + 4");
        assertEquals(0, new BigDecimal("7").compareTo(bal.getQuantity()));
        assertEquals("PURCHASE", saved.getMovementType());
        assertEquals(sourceId, saved.getSourceId());
        assertEquals(0, new BigDecimal("2.5000").compareTo(saved.getUnitCost()));
        assertEquals(movement, saved);
    }

    @Test
    void addStock_cantidadInvalida_lanzaInvalidQuantity() {
        // When / Then
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                stockService.addStock(variantId, warehouseId, BigDecimal.ZERO,
                        null, "PURCHASE", "GOODS_RECEIPT", UUID.randomUUID(), null, actorId, null));
        assertEquals("INVALID_QUANTITY", ex.getCode());
        verifyNoInteractions(stockBalanceRepository);
    }

    @Test
    void addStock_sinBalancePrevio_creaBalanceEnCero() {
        // Given: no existe balance para la variante en la bodega
        when(stockBalanceRepository.findByIdForUpdate(any(StockBalanceId.class))).thenReturn(Optional.empty());
        when(stockBalanceRepository.save(any(StockBalance.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        StockMovement movement = stockService.addStock(variantId, warehouseId, new BigDecimal("5"),
                null, "INITIAL_STOCK", "MANUAL", UUID.randomUUID(), null, actorId, null);

        // Then
        assertEquals(0, new BigDecimal("5").compareTo(movement.getBalanceAfter()));
    }

    @Test
    void deductStock_conWarehouseNula_usaLaBodegaPrincipal() {
        // Given
        UUID mainWarehouseId = UUID.randomUUID();
        Warehouse main = new Warehouse();
        main.setId(mainWarehouseId);
        when(warehouseRepository.findFirstByIsMainTrueAndActiveTrue()).thenReturn(Optional.of(main));
        StockBalance bal = new StockBalance(new StockBalanceId(variantId, mainWarehouseId),
                new BigDecimal("10"), BigDecimal.ZERO);
        when(stockBalanceRepository.findByIdForUpdate(new StockBalanceId(variantId, mainWarehouseId)))
                .thenReturn(Optional.of(bal));
        when(stockBalanceRepository.save(any(StockBalance.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        StockMovement movement = stockService.deductStock(variantId, null, BigDecimal.ONE,
                "SALE", "POS_SALE", UUID.randomUUID(), null, actorId, null);

        // Then
        assertEquals(mainWarehouseId, movement.getWarehouseId());
        verify(warehouseRepository).findFirstByIsMainTrueAndActiveTrue();
    }

    @Test
    void getAvailableStock_devuelveDisponibleSinReservar() {
        // Given
        StockBalance bal = new StockBalance(new StockBalanceId(variantId, warehouseId),
                new BigDecimal("10"), new BigDecimal("3"));
        when(stockBalanceRepository.findById(any(StockBalanceId.class))).thenReturn(Optional.of(bal));

        // When
        BigDecimal available = stockService.getAvailableStock(variantId, warehouseId);

        // Then
        assertEquals(0, new BigDecimal("7").compareTo(available));
    }

    @Test
    void getAvailableStock_sinBalance_devuelveCero() {
        // Given
        when(stockBalanceRepository.findById(any(StockBalanceId.class))).thenReturn(Optional.empty());

        // When / Then
        assertEquals(0, BigDecimal.ZERO.compareTo(stockService.getAvailableStock(variantId, warehouseId)));
    }
}
