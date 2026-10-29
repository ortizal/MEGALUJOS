package ec.com.megalujos.customers.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.UUID;

public class CustomerVehicleRequest {

    private String plate;

    private UUID vehicleVersionId;

    private String brandName;

    private String modelName;

    @Min(value = 1900, message = "El año debe ser igual o mayor a 1900")
    @Max(value = 2100, message = "El año no puede ser mayor a 2100")
    private Integer year;

    private String color;

    private String notes;

    public CustomerVehicleRequest() {
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
}
