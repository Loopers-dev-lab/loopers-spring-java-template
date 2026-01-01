package com.loopers.core.domain.product;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.product.vo.*;
import lombok.Getter;

@Getter
public class MonthlyProductMetric {

    private final MonthlyProductMetricId id;

    private final ProductId productId;

    private final ProductLikeCount likeCount;

    private final ProductDetailViewCount viewCount;

    private final ProductTotalSalesCount totalSalesCount;

    private final YearMonth yearMonth;

    private final CreatedAt createdAt;

    private final UpdatedAt updatedAt;

    private MonthlyProductMetric(
            MonthlyProductMetricId id,
            ProductId productId,
            ProductLikeCount likeCount,
            ProductDetailViewCount viewCount,
            ProductTotalSalesCount totalSalesCount,
            YearMonth yearMonth,
            CreatedAt createdAt,
            UpdatedAt updatedAt
    ) {
        this.id = id;
        this.productId = productId;
        this.likeCount = likeCount;
        this.viewCount = viewCount;
        this.totalSalesCount = totalSalesCount;
        this.yearMonth = yearMonth;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static MonthlyProductMetric create(
            ProductId productId,
            ProductLikeCount likeCount,
            ProductDetailViewCount viewCount,
            ProductTotalSalesCount totalSalesCount,
            YearMonth yearMonth
    ) {
        return new MonthlyProductMetric(
                MonthlyProductMetricId.empty(),
                productId,
                likeCount,
                viewCount,
                totalSalesCount,
                yearMonth,
                CreatedAt.now(),
                UpdatedAt.now()
        );
    }

    public static MonthlyProductMetric mappedBy(
            MonthlyProductMetricId id,
            ProductId productId,
            ProductLikeCount likeCount,
            ProductDetailViewCount viewCount,
            ProductTotalSalesCount totalSalesCount,
            YearMonth yearMonth,
            CreatedAt createdAt,
            UpdatedAt updatedAt
    ) {
        return new MonthlyProductMetric(id, productId, likeCount, viewCount, totalSalesCount, yearMonth, createdAt, updatedAt);
    }
}
