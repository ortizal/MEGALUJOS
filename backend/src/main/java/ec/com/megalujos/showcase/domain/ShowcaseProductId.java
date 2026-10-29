package ec.com.megalujos.showcase.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ShowcaseProductId implements Serializable {

    @Column(name = "showcase_id")
    private UUID showcaseId;

    @Column(name = "variant_id")
    private UUID variantId;

    public ShowcaseProductId() {
    }

    public ShowcaseProductId(UUID showcaseId, UUID variantId) {
        this.showcaseId = showcaseId;
        this.variantId = variantId;
    }

    public UUID getShowcaseId() {
        return showcaseId;
    }

    public void setShowcaseId(UUID showcaseId) {
        this.showcaseId = showcaseId;
    }

    public UUID getVariantId() {
        return variantId;
    }

    public void setVariantId(UUID variantId) {
        this.variantId = variantId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShowcaseProductId that = (ShowcaseProductId) o;
        return Objects.equals(showcaseId, that.showcaseId) && Objects.equals(variantId, that.variantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(showcaseId, variantId);
    }
}
