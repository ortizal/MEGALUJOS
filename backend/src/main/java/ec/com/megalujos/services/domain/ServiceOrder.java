package ec.com.megalujos.services.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Orden de servicio (stickers, instalaciones u otros trabajos). */
@Entity
@Table(name = "service_orders")
public class ServiceOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_number", nullable = false, unique = true, length = 40)
    private String orderNumber;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "vehicle_id")
    private UUID vehicleId;

    @Column(name = "service_type", nullable = false, length = 40)
    private String serviceType; // STICKER, INSTALLATION, OTHER

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String dimensions;

    @Column(nullable = false, length = 30)
    private String status = "QUOTED"; // QUOTED, CONFIRMED, IN_PROGRESS, READY_FOR_DELIVERY, DELIVERED, CANCELLED

    @Column(name = "assigned_user_id")
    private UUID assignedUserId;

    @Column(name = "labor_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal laborAmount = BigDecimal.ZERO;

    /** Total estimado (mano de obra + ítems + IVA resuelto en BD); la tabla no guarda impuestos. */
    @Column(name = "estimated_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal estimatedTotal = BigDecimal.ZERO;

    @Column(name = "scheduled_at")
    private OffsetDateTime scheduledAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @OneToMany(mappedBy = "serviceOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ServiceOrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "serviceOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ServiceMaterial> materials = new ArrayList<>();

    @OneToMany(mappedBy = "serviceOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ServiceStatusHistory> statusHistory = new ArrayList<>();

    @OneToMany(mappedBy = "serviceOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ServiceAdvance> advances = new ArrayList<>();

    // service_attachments usa clave compuesta (@IdClass): se consulta por ServiceAttachmentRepository

    public ServiceOrder() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public UUID getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(UUID vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDimensions() {
        return dimensions;
    }

    public void setDimensions(String dimensions) {
        this.dimensions = dimensions;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UUID getAssignedUserId() {
        return assignedUserId;
    }

    public void setAssignedUserId(UUID assignedUserId) {
        this.assignedUserId = assignedUserId;
    }

    public BigDecimal getLaborAmount() {
        return laborAmount;
    }

    public void setLaborAmount(BigDecimal laborAmount) {
        this.laborAmount = laborAmount;
    }

    public BigDecimal getEstimatedTotal() {
        return estimatedTotal;
    }

    public void setEstimatedTotal(BigDecimal estimatedTotal) {
        this.estimatedTotal = estimatedTotal;
    }

    public OffsetDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(OffsetDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<ServiceOrderItem> getItems() {
        return items;
    }

    public void setItems(List<ServiceOrderItem> items) {
        this.items = items;
    }

    public List<ServiceMaterial> getMaterials() {
        return materials;
    }

    public void setMaterials(List<ServiceMaterial> materials) {
        this.materials = materials;
    }

    public List<ServiceStatusHistory> getStatusHistory() {
        return statusHistory;
    }

    public void setStatusHistory(List<ServiceStatusHistory> statusHistory) {
        this.statusHistory = statusHistory;
    }

    public List<ServiceAdvance> getAdvances() {
        return advances;
    }

    public void setAdvances(List<ServiceAdvance> advances) {
        this.advances = advances;
    }
}
