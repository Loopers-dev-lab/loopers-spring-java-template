package com.loopers.core.domain.product;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.order.vo.Quantity;
import com.loopers.core.domain.product.vo.ProductDetailViewCount;
import com.loopers.core.domain.product.vo.ProductId;
import com.loopers.core.domain.product.vo.ProductMetricId;
import com.loopers.core.domain.product.vo.ProductTotalSalesCount;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
public class ProductMetric {

    private final ProductMetricId id;

    private final ProductId productId;

    private final ProductTotalSalesCount totalSalesCount;

    private final ProductDetailViewCount viewCount;

    private final CreatedAt createdAt;

    private final UpdatedAt updatedAt;

    @Builder(access = AccessLevel.PRIVATE, toBuilder = true)
    private ProductMetric(
            ProductMetricId id,
            ProductId productId,
            ProductTotalSalesCount totalSalesCount,
            ProductDetailViewCount viewCount,
            CreatedAt createdAt,
            UpdatedAt updatedAt
    ) {
        this.id = id;
        this.productId = productId;
        this.totalSalesCount = totalSalesCount;
        this.viewCount = viewCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ProductMetric init(ProductId productId) {
        return new ProductMetric(
                ProductMetricId.empty(),
                productId,
                ProductTotalSalesCount.init(),
                ProductDetailViewCount.init(),
                CreatedAt.now(),
                UpdatedAt.now()
        );
    }

    public static ProductMetric mappedBy(
            ProductMetricId id,
            ProductId productId,
            ProductTotalSalesCount totalSalesCount,
            ProductDetailViewCount viewCount,
            CreatedAt createdAt,
            UpdatedAt updatedAt
    ) {
        return new ProductMetric(id, productId, totalSalesCount, viewCount, createdAt, updatedAt);
    }

    public ProductMetric increaseViewCount() {
        return this.toBuilder()
                .viewCount(this.viewCount.increase())
                .updatedAt(UpdatedAt.now())
                .build();
    }

    public ProductMetric increaseSalesCount(Quantity quantity) {
        return this.toBuilder()
                .totalSalesCount(this.totalSalesCount.increase(quantity))
                .updatedAt(UpdatedAt.now())
                .build();
    }
}
