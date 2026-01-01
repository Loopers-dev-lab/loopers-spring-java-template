package com.loopers.domain.rank;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Monthly product ranking entity for materialized view.
 *
 * <p>This table stores pre-aggregated monthly ranking data for performance optimization.
 */
@Entity
@Table(name = "mv_product_rank_monthly")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MonthlyProductRank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

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
}
