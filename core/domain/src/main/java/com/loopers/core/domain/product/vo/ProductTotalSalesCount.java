package com.loopers.core.domain.product.vo;

public record ProductTotalSalesCount(Long value) {

    public static ProductTotalSalesCount init() {
        return new ProductTotalSalesCount(0L);
    }
}
