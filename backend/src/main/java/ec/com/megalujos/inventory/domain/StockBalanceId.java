package ec.com.megalujos.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class StockBalanceId implements Serializable {

    @Column(name = "variant_id")
    private UUID variantId;

    @Column(name = "warehouse_id")
    private UUID warehouseId;

    public StockBalanceId() {
    }

    public StockBalanceId(UUID variantId, UUID warehouseId) {
        this.variantId = variantId;
        this.warehouseId = warehouseId;
    }

    public UUID getVariantId() {
        return variantId;
    }

    public void setVariantId(UUID variantId) {
        this.variantId = variantId;
    }

    public UUID getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(UUID warehouseId) {
        this.warehouseId = warehouseId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StockBalanceId that = (StockBalanceId) o;
        return Objects.equals(variantId, that.variantId) && Objects.equals(warehouseId, that.warehouseId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variantId, warehouseId);
    }
}
