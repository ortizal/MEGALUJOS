package ec.com.megalujos.services.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

/** Material previsto para la orden; el stock solo se descuenta al consumir. */
@Entity
@Table(name = "service_materials")
public class ServiceMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_order_id", nullable = false)
    @JsonIgnore
    private ServiceOrder serviceOrder;

    @Column(name = "variant_id", nullable = false)
    private UUID variantId;

    @Column(name = "planned_quantity", nullable = false, precision = 18, scale = 3)
    private BigDecimal plannedQuantity = BigDecimal.ZERO;

    @Column(name = "consumed_quantity", nullable = false, precision = 18, scale = 3)
    private BigDecimal consumedQuantity = BigDecimal.ZERO;

    public ServiceMaterial() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ServiceOrder getServiceOrder() {
        return serviceOrder;
    }

    public void setServiceOrder(ServiceOrder serviceOrder) {
        this.serviceOrder = serviceOrder;
    }

    public UUID getVariantId() {
        return variantId;
    }

    public void setVariantId(UUID variantId) {
        this.variantId = variantId;
    }

    public BigDecimal getPlannedQuantity() {
        return plannedQuantity;
    }

    public void setPlannedQuantity(BigDecimal plannedQuantity) {
        this.plannedQuantity = plannedQuantity;
    }

    public BigDecimal getConsumedQuantity() {
        return consumedQuantity;
    }

    public void setConsumedQuantity(BigDecimal consumedQuantity) {
        this.consumedQuantity = consumedQuantity;
    }
}
