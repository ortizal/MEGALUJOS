package ec.com.megalujos.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ProductVehicleCompatibilityId implements Serializable {

    @Column(name = "variant_id")
    private UUID variantId;

    @Column(name = "vehicle_version_id")
    private UUID vehicleVersionId;

    public ProductVehicleCompatibilityId() {
    }

    public ProductVehicleCompatibilityId(UUID variantId, UUID vehicleVersionId) {
        this.variantId = variantId;
        this.vehicleVersionId = vehicleVersionId;
    }

    public UUID getVariantId() {
        return variantId;
    }

    public void setVariantId(UUID variantId) {
        this.variantId = variantId;
    }

    public UUID getVehicleVersionId() {
        return vehicleVersionId;
    }

    public void setVehicleVersionId(UUID vehicleVersionId) {
        this.vehicleVersionId = vehicleVersionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductVehicleCompatibilityId that = (ProductVehicleCompatibilityId) o;
        return Objects.equals(variantId, that.variantId) && Objects.equals(vehicleVersionId, that.vehicleVersionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variantId, vehicleVersionId);
    }
}
