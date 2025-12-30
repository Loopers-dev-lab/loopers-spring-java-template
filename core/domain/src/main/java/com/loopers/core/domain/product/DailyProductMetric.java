package com.loopers.core.domain.product;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.order.vo.Quantity;
import com.loopers.core.domain.product.vo.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(access = AccessLevel.PRIVATE, toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DailyProductMetric {

    private final DailyProductMetricId id;
    private final ProductId productId;
    private final ProductLikeCount likeCount;
    private final ProductTotalSalesCount totalSalesCount;
    private final ProductDetailViewCount viewCount;
    private final CreatedAt createdAt;
    private final UpdatedAt updatedAt;

    public static DailyProductMetric init(ProductId productId) {
        return new DailyProductMetric(
                DailyProductMetricId.empty(),
                productId,
                ProductLikeCount.init(),
                ProductTotalSalesCount.init(),
                ProductDetailViewCount.init(),
                CreatedAt.now(),
                UpdatedAt.now());
    }

    public static DailyProductMetric mappedBy(
            DailyProductMetricId id,
            ProductId productId,
            ProductLikeCount productLikeCount,
            ProductTotalSalesCount totalSalesCount,
            ProductDetailViewCount viewCount,
            CreatedAt createdAt,
            UpdatedAt updatedAt
    ) {
        return new DailyProductMetric(id, productId, productLikeCount, totalSalesCount, viewCount, createdAt, updatedAt);
    }

    public DailyProductMetric increaseViewCount() {
        return this.toBuilder()
                .viewCount(this.viewCount.increase())
                .updatedAt(UpdatedAt.now())
                .build();
    }

    public DailyProductMetric increaseSalesCount(Quantity quantity) {
        return this.toBuilder()
                .totalSalesCount(this.totalSalesCount.increase(quantity))
                .updatedAt(UpdatedAt.now())
                .build();
    }

    public DailyProductMetric increaseLikeCount() {
        return this.toBuilder()
                .likeCount(this.likeCount.increase())
                .build();
    }
}
