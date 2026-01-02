package com.loopers.infrastructure.ranking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "mv_product_rank_monthly", indexes = {
        @Index(name = "idx_monthly_period_rank", columnList = "periodStart, ranking")
})
public class ProductRankMonthly {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private LocalDate periodStart;

    @Column(nullable = false)
    private LocalDate periodEnd;

    @Column(nullable = false)
    private Integer ranking;

    @Column(nullable = false)
    private Long score;

    private Long viewCount;
    private Long likeCount;
    private Long orderCount;
    private Long totalQuantity;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected ProductRankMonthly() {
    }

    private ProductRankMonthly(Long productId, LocalDate periodStart, LocalDate periodEnd,
                               Integer ranking, Long score,
                               Long viewCount, Long likeCount, Long orderCount, Long totalQuantity) {
        this.productId = productId;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.ranking = ranking;
        this.score = score;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.orderCount = orderCount;
        this.totalQuantity = totalQuantity;
        this.createdAt = LocalDateTime.now();
    }

    public static ProductRankMonthly create(Long productId, LocalDate periodStart, LocalDate periodEnd,
                                            Integer ranking, Long score,
                                            Long viewCount, Long likeCount, Long orderCount, Long totalQuantity) {
        return new ProductRankMonthly(productId, periodStart, periodEnd, ranking, score,
                viewCount, likeCount, orderCount, totalQuantity);
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public Integer getRanking() {
        return ranking;
    }

    public Long getScore() {
        return score;
    }

    public Long getViewCount() {
        return viewCount;
    }

    public Long getLikeCount() {
        return likeCount;
    }

    public Long getOrderCount() {
        return orderCount;
    }

    public Long getTotalQuantity() {
        return totalQuantity;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
