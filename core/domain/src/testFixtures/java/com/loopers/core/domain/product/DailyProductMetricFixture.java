package com.loopers.core.domain.product;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.product.vo.*;
import org.instancio.Instancio;

import java.time.LocalDateTime;

import static org.instancio.Select.field;

public class DailyProductMetricFixture {

    public static DailyProductMetric create() {
        return Instancio.of(DailyProductMetric.class)
                .set(field(DailyProductMetric::getId), DailyProductMetricId.empty())
                .set(field(DailyProductMetric::getProductId), new ProductId("1"))
                .set(field(DailyProductMetric::getLikeCount), new ProductLikeCount(0L))
                .set(field(DailyProductMetric::getTotalSalesCount), new ProductTotalSalesCount(0L))
                .set(field(DailyProductMetric::getViewCount), new ProductDetailViewCount(0L))
                .set(field(DailyProductMetric::getCreatedAt), new CreatedAt(LocalDateTime.now()))
                .set(field(DailyProductMetric::getUpdatedAt), new UpdatedAt(LocalDateTime.now()))
                .create();
    }

    public static DailyProductMetric createWith(String productId) {
        return Instancio.of(DailyProductMetric.class)
                .set(field(DailyProductMetric::getId), DailyProductMetricId.empty())
                .set(field(DailyProductMetric::getProductId), new ProductId(productId))
                .set(field(DailyProductMetric::getLikeCount), new ProductLikeCount(0L))
                .set(field(DailyProductMetric::getTotalSalesCount), new ProductTotalSalesCount(0L))
                .set(field(DailyProductMetric::getViewCount), new ProductDetailViewCount(0L))
                .set(field(DailyProductMetric::getCreatedAt), new CreatedAt(LocalDateTime.now()))
                .set(field(DailyProductMetric::getUpdatedAt), new UpdatedAt(LocalDateTime.now()))
                .create();
    }

    public static DailyProductMetric createWith(String productId, LocalDateTime createdAt) {
        return Instancio.of(DailyProductMetric.class)
                .set(field(DailyProductMetric::getId), DailyProductMetricId.empty())
                .set(field(DailyProductMetric::getProductId), new ProductId(productId))
                .set(field(DailyProductMetric::getLikeCount), new ProductLikeCount(0L))
                .set(field(DailyProductMetric::getTotalSalesCount), new ProductTotalSalesCount(0L))
                .set(field(DailyProductMetric::getViewCount), new ProductDetailViewCount(0L))
                .set(field(DailyProductMetric::getCreatedAt), new CreatedAt(createdAt))
                .set(field(DailyProductMetric::getUpdatedAt), new UpdatedAt(createdAt))
                .create();
    }

    public static DailyProductMetric createWith(
            String productId,
            long likeCount,
            long viewCount,
            long totalSalesCount,
            LocalDateTime createdAt
    ) {
        return Instancio.of(DailyProductMetric.class)
                .set(field(DailyProductMetric::getId), DailyProductMetricId.empty())
                .set(field(DailyProductMetric::getProductId), new ProductId(productId))
                .set(field(DailyProductMetric::getLikeCount), new ProductLikeCount(likeCount))
                .set(field(DailyProductMetric::getTotalSalesCount), new ProductTotalSalesCount(totalSalesCount))
                .set(field(DailyProductMetric::getViewCount), new ProductDetailViewCount(viewCount))
                .set(field(DailyProductMetric::getCreatedAt), new CreatedAt(createdAt))
                .set(field(DailyProductMetric::getUpdatedAt), new UpdatedAt(createdAt))
                .create();
    }
}
