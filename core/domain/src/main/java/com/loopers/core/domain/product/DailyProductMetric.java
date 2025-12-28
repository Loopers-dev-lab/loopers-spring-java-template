package com.loopers.core.domain.product;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.order.vo.Quantity;
import com.loopers.core.domain.product.vo.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
public class DailyProductMetric {

    private final ProductMetricId id;

    private final ProductId productId;

    private final ProductLikeCount likeCount;

    private final ProductTotalSalesCount totalSalesCount;

    private final ProductDetailViewCount viewCount;

    private final CreatedAt createdAt;

    private final UpdatedAt updatedAt;

    @Builder(access = AccessLevel.PRIVATE, toBuilder = true)
    private DailyProductMetric(
            ProductMetricId id,
            ProductId productId,
            ProductLikeCount likeCount,
            ProductTotalSalesCount totalSalesCount,
            ProductDetailViewCount viewCount,
            CreatedAt createdAt,
            UpdatedAt updatedAt
    ) {
        this.id = id;
        this.productId = productId;
        this.likeCount = likeCount;
        this.totalSalesCount = totalSalesCount;
        this.viewCount = viewCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static DailyProductMetric init(ProductId productId) {
        return new DailyProductMetric(
                ProductMetricId.empty(),
                productId,
                ProductLikeCount.init(),
                ProductTotalSalesCount.init(),
                ProductDetailViewCount.init(),
                CreatedAt.now(),
                UpdatedAt.now());
    }

    public static DailyProductMetric mappedBy(
            ProductMetricId id,
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
