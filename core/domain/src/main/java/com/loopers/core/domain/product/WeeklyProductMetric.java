package com.loopers.core.domain.product;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.common.vo.YearMonthWeek;
import com.loopers.core.domain.product.vo.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class WeeklyProductMetric {

    private final WeeklyProductMetricId id;
    private final ProductId productId;
    private final ProductLikeCount likeCount;
    private final ProductDetailViewCount viewCount;
    private final ProductTotalSalesCount totalSalesCount;
    private final YearMonthWeek yearMonthWeek;
    private final CreatedAt createdAt;
    private final UpdatedAt updatedAt;

    public static WeeklyProductMetric create(
            ProductId productId,
            ProductLikeCount likeCount,
            ProductDetailViewCount viewCount,
            ProductTotalSalesCount totalSalesCount,
            YearMonthWeek yearMonthWeek
    ) {
        return new WeeklyProductMetric(
                WeeklyProductMetricId.empty(),
                productId,
                likeCount,
                viewCount,
                totalSalesCount,
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
