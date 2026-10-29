package ec.com.megalujos.inventory.domain;

import java.math.BigDecimal;
import java.util.UUID;

/** Proyección de lectura de existencias por variante/bodega con datos de producto. */
public interface StockBalanceRow {

    UUID getVariantId();

    UUID getWarehouseId();

    BigDecimal getQuantity();

    BigDecimal getReserved();

    BigDecimal getMinStock();

    String getSku();

    String getProductName();

    String getWarehouseName();
}
