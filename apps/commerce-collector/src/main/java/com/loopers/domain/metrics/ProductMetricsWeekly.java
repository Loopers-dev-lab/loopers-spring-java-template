package com.loopers.domain.metrics;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.ZonedDateTime;

/**
 * 주간 상품 집계 Materialized View
 * Spring Batch를 통해 ProductMetricsDaily 데이터를 주 단위로 집계
 */
@Entity
@Table(
        name = "mv_product_metrics_weekly",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_product_year_week",
                        columnNames = {"product_id", "year", "week"}
                )
        },
        indexes = {
                @Index(name = "idx_year_week", columnList = "year, week"),
                @Index(name = "idx_product_id", columnList = "product_id")
        }
)
@Getter
@NoArgsConstructor
public class ProductMetricsWeekly extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "week", nullable = false)
    private Integer week;

    /**
     * 집계 기간 시작일 (해당 주의 월요일)
     */
    @Column(name = "period_start_date", nullable = false)
    private LocalDate periodStartDate;

    /**
     * 집계 기간 종료일 (해당 주의 일요일)
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
     * 주간 집계 생성
     */
    public static ProductMetricsWeekly create(
            Long productId,
            Integer year,
            Integer week,
            LocalDate periodStartDate,
            LocalDate periodEndDate
    ) {
        ProductMetricsWeekly metrics = new ProductMetricsWeekly();
        metrics.productId = productId;
        metrics.year = year;
        metrics.week = week;
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
