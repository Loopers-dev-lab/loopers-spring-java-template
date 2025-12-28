package com.loopers.core.infra.database.mysql.product.entity;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.product.DailyProductMetric;
import com.loopers.core.domain.product.vo.*;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Entity
@Table(
        name = "daily_product_metrics",
        indexes = {
                @Index(name = "idx_product_metric_product_id", columnList = "product_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DailyProductMetricEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Long likeCount;

    @Column(nullable = false)
    private Long totalSalesCount;

    @Column(nullable = false)
    private Long viewCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static DailyProductMetricEntity from(DailyProductMetric metric) {
        return new DailyProductMetricEntity(
                Optional.ofNullable(metric.getId().value())
                        .map(Long::parseLong)
                        .orElse(null),
                Long.parseLong(Objects.requireNonNull(metric.getProductId().value())),
                metric.getLikeCount().value(),
                metric.getTotalSalesCount().value(),
                metric.getViewCount().value(),
                metric.getCreatedAt().value(),
                metric.getUpdatedAt().value()
        );
    }

    public DailyProductMetric to() {
        return DailyProductMetric.mappedBy(
                new ProductMetricId(this.id.toString()),
                new ProductId(this.productId.toString()),
                new ProductLikeCount(this.likeCount),
                new ProductTotalSalesCount(this.totalSalesCount),
                new ProductDetailViewCount(this.viewCount),
                new CreatedAt(this.createdAt),
                new UpdatedAt(this.updatedAt)
        );
    }
}
