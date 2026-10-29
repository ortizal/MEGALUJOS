package ec.com.megalujos.catalog.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VehicleCompatibilityRequest {

    @Valid
    private List<Item> items = new ArrayList<>();

    public VehicleCompatibilityRequest() {
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public static class Item {

        @NotNull(message = "El vehicleVersionId es obligatorio")
        private UUID vehicleVersionId;

        private boolean verified = false;

        private String notes;

        public Item() {
        }

        public UUID getVehicleVersionId() {
            return vehicleVersionId;
        }

        public void setVehicleVersionId(UUID vehicleVersionId) {
            this.vehicleVersionId = vehicleVersionId;
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
}
