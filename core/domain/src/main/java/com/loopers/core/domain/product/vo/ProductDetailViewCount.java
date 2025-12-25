package com.loopers.core.domain.product.vo;

public record ProductDetailViewCount(Long value) {

    public static ProductDetailViewCount init() {
        return new ProductDetailViewCount(0L);
    }

    public ProductDetailViewCount increase() {
        return new ProductDetailViewCount(this.value + 1);
    }
}
