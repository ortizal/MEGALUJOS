package ec.com.megalujos.catalog.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "vehicle_models", uniqueConstraints = @UniqueConstraint(columnNames = {"brand_id", "name"}))
public class VehicleModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "brand_id", nullable = false)
    private VehicleBrand brand;

    @Column(nullable = false, length = 120)
    private String name;

    public VehicleModel() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public VehicleBrand getBrand() {
        return brand;
    }

    public void setBrand(VehicleBrand brand) {
        this.brand = brand;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
