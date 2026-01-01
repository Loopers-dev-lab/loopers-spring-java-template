package com.loopers.domain.rank;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Materialized View를 위한 월간 상품 랭킹 엔티티.
 *
 * <p>이 테이블은 성능 최적화를 위해 사전 집계된 월간 랭킹 데이터를 저장합니다.
 * 집계는 Spring Batch Job에 의해 수행되며 빠른 조회를 위해 여기에 저장됩니다.
 *
 * @see com.loopers.batch.config.MonthlyRankingJobConfig
 */
@Entity
@Table(
    name = "mv_product_rank_monthly",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_product_month",
        columnNames = {"product_id", "year_month"}
    ),
    indexes = {
        @Index(name = "idx_year_month_rank", columnList = "year_month, rank_position"),
        @Index(name = "idx_year_month_score", columnList = "year_month, total_score DESC")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MonthlyProductRank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    /**
     * 년-월 형식: YYYY-MM (예: "2025-01")
     */
    @Column(name = "`year_month`", nullable = false, length = 7)
    private String yearMonth;

    @Column(name = "rank_position", nullable = false)
    private Integer rankPosition;

    @Column(name = "total_score", nullable = false)
    private Double totalScore;

    @Column(name = "like_count", nullable = false)
    private Integer likeCount;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount;

    @Column(name = "order_count", nullable = false)
    private Integer orderCount;

    @Column(name = "sales_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal salesAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public MonthlyProductRank(
        Long productId,
        String yearMonth,
        Integer rankPosition,
        Double totalScore,
        Integer likeCount,
        Integer viewCount,
        Integer orderCount,
        BigDecimal salesAmount
    ) {
        this.productId = productId;
        this.yearMonth = yearMonth;
        this.rankPosition = rankPosition;
        this.totalScore = totalScore;
        this.likeCount = likeCount;
        this.viewCount = viewCount;
        this.orderCount = orderCount;
        this.salesAmount = salesAmount;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
