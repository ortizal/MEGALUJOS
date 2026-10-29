package ec.com.megalujos.catalog.dto;

import java.util.UUID;

public class VehicleCompatibilityResponse {

    private UUID vehicleVersionId;
    private String vehicleVersionName;
    private boolean verified;
    private String notes;

    public VehicleCompatibilityResponse() {
    }

    public UUID getVehicleVersionId() {
        return vehicleVersionId;
    }

    public void setVehicleVersionId(UUID vehicleVersionId) {
        this.vehicleVersionId = vehicleVersionId;
    }

    public String getVehicleVersionName() {
        return vehicleVersionName;
    }

    public void setVehicleVersionName(String vehicleVersionName) {
        this.vehicleVersionName = vehicleVersionName;
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
