package com.loopers.core.domain.productlike.vo;

public record ProductLikeId(String value) {

    public static ProductLikeId empty() {
        return new ProductLikeId(null);
    }
}
