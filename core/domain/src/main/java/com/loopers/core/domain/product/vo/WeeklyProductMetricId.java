package com.loopers.core.domain.product.vo;

public record WeeklyProductMetricId(String value) {

    public static WeeklyProductMetricId empty() {
        return new WeeklyProductMetricId(null);
    }
}
