package com.loopers.core.domain.product.vo;

public record ProductMetricId(String value) {

    public static ProductMetricId empty() {
        return new ProductMetricId(null);
    }
}
