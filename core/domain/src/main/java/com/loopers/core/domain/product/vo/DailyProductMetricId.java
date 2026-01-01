package com.loopers.core.domain.product.vo;

public record DailyProductMetricId(String value) {

    public static DailyProductMetricId empty() {
        return new DailyProductMetricId(null);
    }
}
