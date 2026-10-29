package ec.com.megalujos.purchasing.domain;

import java.math.BigDecimal;
import java.util.UUID;

/** Proyección de lectura: cantidad ya recibida por línea de orden de compra. */
public interface ReceivedQuantityRow {

    UUID getOrderItemId();

    BigDecimal getQuantity();
}
