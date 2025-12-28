package com.loopers.core.domain.product;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.common.vo.YearMonthWeek;
import com.loopers.core.domain.product.vo.*;
import lombok.Getter;

@Getter
public class WeeklyProductMetric {

    private final WeeklyProductMetricId id;

    private final ProductId productId;

    private final ProductLikeCount likeCount;

    private final ProductDetailViewCount viewCount;

    private final ProductTotalSalesCount totalSalesCount;

    private final YearMonthWeek yearMonthWeek;

    private final CreatedAt createdAt;

    private final UpdatedAt updatedAt;

    private WeeklyProductMetric(
            WeeklyProductMetricId id,
            ProductId productId,
            ProductLikeCount likeCount,
            ProductDetailViewCount viewCount,
            ProductTotalSalesCount totalSalesCount,
            YearMonthWeek yearMonthWeek,
            CreatedAt createdAt,
            UpdatedAt updatedAt
    ) {
        this.id = id;
        this.productId = productId;
        this.likeCount = likeCount;
        this.viewCount = viewCount;
        this.totalSalesCount = totalSalesCount;
        this.yearMonthWeek = yearMonthWeek;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static WeeklyProductMetric create(
            ProductId productId,
            YearMonthWeek yearMonthWeek
    ) {
        return new WeeklyProductMetric(
                WeeklyProductMetricId.empty(),
                productId,
                ProductLikeCount.init(),
                ProductDetailViewCount.init(),
                ProductTotalSalesCount.init(),
                yearMonthWeek,
                CreatedAt.now(),
                UpdatedAt.now()
        );
    }

    public static WeeklyProductMetric mappedBy(
            WeeklyProductMetricId id,
            ProductId productId,
            ProductLikeCount likeCount,
            ProductDetailViewCount viewCount,
            ProductTotalSalesCount totalSalesCount,
            YearMonthWeek yearMonthWeek,
            CreatedAt createdAt,
            UpdatedAt updatedAt
    ) {
        return new WeeklyProductMetric(id, productId, likeCount, viewCount, totalSalesCount, yearMonthWeek, createdAt, updatedAt);
    }
}
