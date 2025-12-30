package com.loopers.core.infra.database.mysql.product.entity;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.common.vo.YearMonthWeek;
import com.loopers.core.domain.product.WeeklyProductMetric;
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
        name = "weekly_product_metrics",
        indexes = {
                @Index(name = "idx_product_metric_product_id", columnList = "product_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class WeeklyProductMetricEntity {

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
    private Integer weekOfYear;

    @Column(nullable = false)
    private Long likeCount;

    @Column(nullable = false)
    private Long totalSalesCount;

    @Column(nullable = false)
    private Long viewCount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static WeeklyProductMetricEntity from(WeeklyProductMetric metric) {
        return new WeeklyProductMetricEntity(
                Optional.ofNullable(metric.getProductId().value())
                        .map(Long::parseLong)
                        .orElse(null),
                Long.parseLong(Objects.requireNonNull(metric.getProductId().value())),
                metric.getYearMonthWeek().year(),
                metric.getYearMonthWeek().month(),
                metric.getYearMonthWeek().weekOfYear(),
                metric.getLikeCount().value(),
                metric.getTotalSalesCount().value(),
                metric.getViewCount().value(),
                metric.getCreatedAt().value(),
                metric.getUpdatedAt().value()
        );
    }

    public WeeklyProductMetric to() {
        return WeeklyProductMetric.mappedBy(
                new WeeklyProductMetricId(this.id.toString()),
                new ProductId(this.productId.toString()),
                new ProductLikeCount(this.likeCount),
                new ProductDetailViewCount(this.viewCount),
                new ProductTotalSalesCount(this.totalSalesCount),
                new YearMonthWeek(this.year, this.month, this.weekOfYear),
                new CreatedAt(this.createdAt),
                new UpdatedAt(this.updatedAt)
        );
    }
}
