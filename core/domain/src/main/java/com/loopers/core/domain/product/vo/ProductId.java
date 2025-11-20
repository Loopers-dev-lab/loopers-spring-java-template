package com.loopers.core.domain.product.vo;

public record ProductId(String value) {

    public static ProductId empty() {
        return new ProductId(null);
    }
}
