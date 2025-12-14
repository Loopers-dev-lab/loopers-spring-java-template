package com.loopers.core.domain.product;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.product.vo.*;
import lombok.Getter;

@Getter
public class ProductMetric {

    private final ProductMetricId id;

    private final ProductId productId;

    private final ProductLikeCount likeCount;

    private final ProductTotalSalesCount totalSalesCount;

    private final ProductDetailViewCount viewCount;

    private final CreatedAt createdAt;

    private final UpdatedAt updatedAt;

    private ProductMetric(
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

    public static ProductMetric init(ProductId productId) {
        return new ProductMetric(
                ProductMetricId.empty(),
                productId,
                ProductLikeCount.init(),
                ProductTotalSalesCount.init(),
                ProductDetailViewCount.init(),
                CreatedAt.now(),
                UpdatedAt.now()
        );
    }

    public static ProductMetric mappedBy(
            ProductMetricId id,
            ProductId productId,
            ProductLikeCount likeCount,
            ProductTotalSalesCount totalSales,
            ProductDetailViewCount viewCount,
            CreatedAt createdAt,
            UpdatedAt updatedAt
    ) {
        return new ProductMetric(id, productId, likeCount, totalSales, viewCount, createdAt, updatedAt);
    }
}
