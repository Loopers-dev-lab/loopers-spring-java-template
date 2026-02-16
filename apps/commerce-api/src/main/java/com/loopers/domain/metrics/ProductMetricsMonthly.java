package com.loopers.domain.metrics;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;
import java.time.ZonedDateTime;

/**
 * 월간 상품 집계 Materialized View (읽기 전용)
 * commerce-collector에서 생성한 집계 데이터 조회용
 */
@Entity
@Table(name = "mv_product_metrics_monthly")
@Getter
@NoArgsConstructor
@Immutable // 읽기 전용
public class ProductMetricsMonthly {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "month", nullable = false)
    private Integer month;

    @Column(name = "period_start_date", nullable = false)
    private LocalDate periodStartDate;

    @Column(name = "period_end_date", nullable = false)
    private LocalDate periodEndDate;

    @Column(name = "total_like_count", nullable = false)
    private Long totalLikeCount;

    @Column(name = "total_view_count", nullable = false)
    private Long totalViewCount;

    @Column(name = "total_order_count", nullable = false)
    private Long totalOrderCount;

    @Column(name = "aggregated_at")
    private ZonedDateTime aggregatedAt;

    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    /**
     * 종합 점수 계산 (가중치 적용)
     * Score = (like * 0.2) + (view * 0.1) + (order * 0.6)
     */
    public double calculateCompositeScore() {
        return (totalLikeCount * 0.2) + (totalViewCount * 0.1) + (totalOrderCount * 0.6);
    }
}
