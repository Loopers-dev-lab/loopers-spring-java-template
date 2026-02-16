package com.loopers.domain.metrics;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.ZonedDateTime;

/**
 * 월간 상품 집계 Materialized View
 * Spring Batch를 통해 ProductMetricsDaily 데이터를 월 단위로 집계
 */
@Entity
@Table(
        name = "mv_product_metrics_monthly",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_product_year_month",
                        columnNames = {"product_id", "year", "month"}
                )
        },
        indexes = {
                @Index(name = "idx_year_month", columnList = "year, month"),
                @Index(name = "idx_product_id", columnList = "product_id")
        }
)
@Getter
@NoArgsConstructor
public class ProductMetricsMonthly extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "month", nullable = false)
    private Integer month;

    /**
     * 집계 기간 시작일 (해당 월의 1일)
     */
    @Column(name = "period_start_date", nullable = false)
    private LocalDate periodStartDate;

    /**
     * 집계 기간 종료일 (해당 월의 말일)
     */
    @Column(name = "period_end_date", nullable = false)
    private LocalDate periodEndDate;

    @Column(name = "total_like_count", nullable = false)
    private Long totalLikeCount = 0L;

    @Column(name = "total_view_count", nullable = false)
    private Long totalViewCount = 0L;

    @Column(name = "total_order_count", nullable = false)
    private Long totalOrderCount = 0L;

    /**
     * 마지막 집계 시각
     */
    @Column(name = "aggregated_at")
    private ZonedDateTime aggregatedAt;

    /**
     * 월간 집계 생성
     */
    public static ProductMetricsMonthly create(
            Long productId,
            Integer year,
            Integer month,
            LocalDate periodStartDate,
            LocalDate periodEndDate
    ) {
        ProductMetricsMonthly metrics = new ProductMetricsMonthly();
        metrics.productId = productId;
        metrics.year = year;
        metrics.month = month;
        metrics.periodStartDate = periodStartDate;
        metrics.periodEndDate = periodEndDate;
        return metrics;
    }

    /**
     * 집계 메트릭 업데이트
     */
    public void updateMetrics(
            Long likeCount,
            Long viewCount,
            Long orderCount
    ) {
        this.totalLikeCount = likeCount;
        this.totalViewCount = viewCount;
        this.totalOrderCount = orderCount;
        this.aggregatedAt = ZonedDateTime.now();
    }

    /**
     * 집계 데이터 초기화 (재집계 시)
     */
    public void reset() {
        this.totalLikeCount = 0L;
        this.totalViewCount = 0L;
        this.totalOrderCount = 0L;
        this.aggregatedAt = null;
    }
}
