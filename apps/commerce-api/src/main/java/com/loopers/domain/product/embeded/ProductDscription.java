package com.loopers.domain.product.embeded;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Lob;

@Embeddable
public class ProductDscription {
    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    public ProductDscription() {

    }

    private ProductDscription(String description) {
        this.description = description;
    }
    public static ProductDscription of(String description) {
        return new ProductDscription(description);
    }

    public String getValue() {
        return this.description;
    }
}
