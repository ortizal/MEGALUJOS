package ec.com.megalujos.showcase.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public class ShowcaseProductsRequest {

    @NotNull
    private List<UUID> variantIds;

    public ShowcaseProductsRequest() {
    }

    public List<UUID> getVariantIds() {
        return variantIds;
    }

    public void setVariantIds(List<UUID> variantIds) {
        this.variantIds = variantIds;
    }
}
