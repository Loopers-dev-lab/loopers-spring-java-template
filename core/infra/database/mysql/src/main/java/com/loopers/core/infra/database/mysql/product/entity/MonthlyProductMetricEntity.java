package com.loopers.core.infra.database.mysql.product.entity;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.product.MonthlyProductMetric;
import com.loopers.core.domain.product.vo.*;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Getter
@Entity
@Table(
        name = "monthly_product_metrics",
        indexes = {
                @Index(name = "idx_product_metric_product_id", columnList = "product_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_monthly_product_metric", columnNames = {"product_id", "year", "month"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MonthlyProductMetricEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long productId;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer month;

    @Column(nullable = false)
    private Long likeCount;

    @Column(nullable = false)
    private Long totalSalesCount;

    @Column(nullable = false)
    private Long viewCount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static MonthlyProductMetricEntity from(MonthlyProductMetric metric) {
        return new MonthlyProductMetricEntity(
                Optional.ofNullable(metric.getId().value())
                        .map(Long::parseLong)
                        .orElse(null),
                Long.parseLong(Objects.requireNonNull(metric.getProductId().value())),
                metric.getYearMonth().year(),
                metric.getYearMonth().month(),
                metric.getLikeCount().value(),
                metric.getTotalSalesCount().value(),
                metric.getViewCount().value(),
                metric.getCreatedAt().value(),
                metric.getUpdatedAt().value()
        );
    }

    public MonthlyProductMetric to() {
        return MonthlyProductMetric.mappedBy(
                new MonthlyProductMetricId(this.id.toString()),
                new ProductId(this.productId.toString()),
                new ProductLikeCount(this.likeCount),
                new ProductDetailViewCount(this.viewCount),
                new ProductTotalSalesCount(this.totalSalesCount),
                new YearMonth(this.year, this.month),
                new CreatedAt(this.createdAt),
                new UpdatedAt(this.updatedAt)
        );
    }
}
