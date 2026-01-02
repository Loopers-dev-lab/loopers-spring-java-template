package com.loopers.domain.ranking;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

/**
 * 시간별 랭킹 점수 누적 엔티티 (슬라이딩 윈도우)
 * 최근 1시간 동안의 Raw Metrics를 누적하고 current_score를 계산
 */
@Entity
@Table(name = "ranking_score_hourly", indexes = {
    @Index(name = "idx_current_score", columnList = "current_score DESC")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class RankingScoreHourly {

    @Id
    @Column(name = "product_id", nullable = false)
    private Long productId;

    // Raw Metrics (누적)
    @Column(name = "total_order_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalOrderAmount;

    @Column(name = "total_like_count", nullable = false)
    private Long totalLikeCount;

    @Column(name = "total_view_count", nullable = false)
    private Long totalViewCount;

    // Calculated Score (인덱스됨, 정렬용)
    @Column(name = "current_score", nullable = false)
    private Double currentScore;

    @Column(name = "last_processed_time")
    private LocalDateTime lastProcessedTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    @PrePersist
    private void prePersist() {
        ZonedDateTime now = ZonedDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        guard();
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = ZonedDateTime.now();
        guard();
    }

    @Builder
    private RankingScoreHourly(Long productId, BigDecimal totalOrderAmount, Long totalLikeCount,
                               Long totalViewCount, Double currentScore, LocalDateTime lastProcessedTime) {
        this.productId = productId;
        this.totalOrderAmount = totalOrderAmount != null ? totalOrderAmount : BigDecimal.ZERO;
        this.totalLikeCount = totalLikeCount != null ? totalLikeCount : 0L;
        this.totalViewCount = totalViewCount != null ? totalViewCount : 0L;
        this.currentScore = currentScore != null ? currentScore : 0.0;
        this.lastProcessedTime = lastProcessedTime;
    }

    private void guard() {
        if (productId == null) {
            throw new IllegalArgumentException("productId는 필수입니다");
        }
    }
}

