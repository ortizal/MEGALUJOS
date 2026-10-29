package ec.com.megalujos.customers.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class CustomerVehicleResponseDto {

    private UUID id;
    private UUID customerId;
    private String plate;
    private UUID vehicleVersionId;
    private String brandName;
    private String modelName;
    private Integer year;
    private String color;
    private String notes;
    private OffsetDateTime createdAt;

    public CustomerVehicleResponseDto() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public String getPlate() {
        return plate;
    }

    public void setPlate(String plate) {
        this.plate = plate;
    }

    public UUID getVehicleVersionId() {
        return vehicleVersionId;
    }

    public void setVehicleVersionId(UUID vehicleVersionId) {
        this.vehicleVersionId = vehicleVersionId;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
