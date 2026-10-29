package ec.com.megalujos.catalog.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ProductAdminRequest {

    @NotBlank(message = "El SKU es obligatorio")
    @Size(max = 60, message = "El SKU no puede exceder 60 caracteres")
    private String sku;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 200, message = "El nombre no puede exceder 200 caracteres")
    private String name;

    @NotNull(message = "La categoría es obligatoria")
    private UUID categoryId;

    private UUID brandId;

    @NotBlank(message = "El tipo de producto es obligatorio")
    @Pattern(regexp = "PHYSICAL|SERVICE", message = "El tipo de producto debe ser PHYSICAL o SERVICE")
    private String productType = "PHYSICAL";

    @Size(max = 20, message = "La unidad no puede exceder 20 caracteres")
    private String unit = "UNIT";

    private boolean trackStock = true;

    private boolean published = false;

    private String description;

    @Valid
    private List<VariantRequest> variants = new ArrayList<>();

    public ProductAdminRequest() {
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }

    public UUID getBrandId() {
        return brandId;
    }

    public void setBrandId(UUID brandId) {
        this.brandId = brandId;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public boolean isTrackStock() {
        return trackStock;
    }

    public void setTrackStock(boolean trackStock) {
        this.trackStock = trackStock;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<VariantRequest> getVariants() {
        return variants;
    }

    public void setVariants(List<VariantRequest> variants) {
        this.variants = variants;
    }

    public static class VariantRequest {

        private UUID id;

        @NotBlank(message = "El SKU de la variante es obligatorio")
        @Size(max = 80, message = "El SKU de la variante no puede exceder 80 caracteres")
        private String sku;

        @Size(max = 80, message = "El código de barras no puede exceder 80 caracteres")
        private String barcode;

        private Map<String, Object> attributes;

        @NotNull(message = "El precio de venta es obligatorio")
        @DecimalMin(value = "0", message = "El precio de venta no puede ser negativo")
        private BigDecimal salePrice;

        @DecimalMin(value = "0", message = "El costo no puede ser negativo")
        private BigDecimal costPrice;

        @DecimalMin(value = "0", message = "El stock mínimo no puede ser negativo")
        private BigDecimal minStock = BigDecimal.ZERO;

        public VariantRequest() {
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getSku() {
            return sku;
        }

        public void setSku(String sku) {
            this.sku = sku;
        }

        public String getBarcode() {
            return barcode;
        }

        public void setBarcode(String barcode) {
            this.barcode = barcode;
        }

        public Map<String, Object> getAttributes() {
            return attributes;
        }

        public void setAttributes(Map<String, Object> attributes) {
            this.attributes = attributes;
        }

        public BigDecimal getSalePrice() {
            return salePrice;
        }

        public void setSalePrice(BigDecimal salePrice) {
            this.salePrice = salePrice;
        }

        public BigDecimal getCostPrice() {
            return costPrice;
        }

        public void setCostPrice(BigDecimal costPrice) {
            this.costPrice = costPrice;
        }

        public BigDecimal getMinStock() {
            return minStock;
        }

        public void setMinStock(BigDecimal minStock) {
            this.minStock = minStock;
        }
    }
}
