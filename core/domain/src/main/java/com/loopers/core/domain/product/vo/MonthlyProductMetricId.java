package com.loopers.core.domain.product.vo;

public record MonthlyProductMetricId(String value) {

    public static MonthlyProductMetricId empty() {
        return new MonthlyProductMetricId(null);
    }
}
