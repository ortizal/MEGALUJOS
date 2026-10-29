package ec.com.megalujos.showcase.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "showcase_products")
public class ShowcaseProduct {

    @EmbeddedId
    private ShowcaseProductId id;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    public ShowcaseProduct() {
    }

    public ShowcaseProduct(ShowcaseProductId id, int sortOrder) {
        this.id = id;
        this.sortOrder = sortOrder;
    }

    public ShowcaseProductId getId() {
        return id;
    }

    public void setId(ShowcaseProductId id) {
        this.id = id;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
