package ec.com.megalujos.showcase.dto;

import jakarta.validation.constraints.Size;

import java.util.UUID;

public class CatalogInquiryRequest {

    private UUID variantId;

    @Size(max = 40)
    private String customerPhone;

    public CatalogInquiryRequest() {
    }

    public UUID getVariantId() {
        return variantId;
    }

    public void setVariantId(UUID variantId) {
        this.variantId = variantId;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }
}
