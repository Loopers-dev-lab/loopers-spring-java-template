package com.loopers.core.domain.product.vo;

import com.loopers.core.domain.common.vo.YearMonthWeek;
import com.loopers.core.domain.product.WeeklyProductMetric;

public record ProductMetricAggregation(
        String productId,
        Long totalLikeCount,
        Long totalViewCount,
        Long totalSalesCount
) {
    public WeeklyProductMetric to(YearMonthWeek yearMonthWeek) {
        return WeeklyProductMetric.create(
                new ProductId(this.productId()),
                new ProductLikeCount(this.totalLikeCount()),
                new ProductDetailViewCount(this.totalViewCount()),
                new ProductTotalSalesCount(this.totalSalesCount()),
                yearMonthWeek
        );
    }
}
