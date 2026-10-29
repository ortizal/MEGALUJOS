package ec.com.megalujos.inventory.domain;

import java.math.BigDecimal;
import java.util.UUID;

/** Proyección de alerta de stock mínimo por variante. */
public interface StockAlertRow {

    UUID getVariantId();

    BigDecimal getQuantity();

    BigDecimal getMinStock();

    String getSku();

    String getProductName();
}
