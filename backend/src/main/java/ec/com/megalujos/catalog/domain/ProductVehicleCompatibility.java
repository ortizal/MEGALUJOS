package ec.com.megalujos.catalog.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "product_vehicle_compatibility")
public class ProductVehicleCompatibility {

    @EmbeddedId
    private ProductVehicleCompatibilityId id;

    @Column(nullable = false)
    private boolean verified = false;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public ProductVehicleCompatibility() {
    }

    public ProductVehicleCompatibility(ProductVehicleCompatibilityId id, boolean verified, String notes) {
        this.id = id;
        this.verified = verified;
        this.notes = notes;
    }

    public ProductVehicleCompatibilityId getId() {
        return id;
    }

    public void setId(ProductVehicleCompatibilityId id) {
        this.id = id;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
