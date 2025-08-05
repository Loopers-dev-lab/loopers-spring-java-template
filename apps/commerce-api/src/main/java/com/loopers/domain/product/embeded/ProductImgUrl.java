package com.loopers.domain.product.embeded;

import jakarta.persistence.Embeddable;

@Embeddable
public class ProductImgUrl {
    private String imgUrl;

    public ProductImgUrl(String productImgUrl) {
        this.imgUrl = productImgUrl;
    }

    public ProductImgUrl() {

    }

    public static ProductImgUrl of(String productImgUrl) {
        return new ProductImgUrl(productImgUrl);
    }

    public String getValue() {
        return this.imgUrl;
    }
}
