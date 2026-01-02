package com.loopers.domain.ranking;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 5분 단위 상품 점수 집계 엔티티
 * RankingEventLog를 5분 단위로 집계한 Raw Metrics를 저장
 */
@Entity
@Table(name = "product_score_5min", indexes = {
    @Index(name = "idx_start_time_end_time", columnList = "start_time, end_time"),
    @Index(name = "idx_product_id_start_time_end_time", columnList = "product_id, start_time, end_time")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ProductScore5Min extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    // Raw Metrics for the 5-minute interval
    @Column(name = "order_amount_sum", nullable = false, precision = 19, scale = 2)
    private BigDecimal orderAmountSum;

    @Column(name = "like_count", nullable = false)
    private Long likeCount;

    @Column(name = "view_count", nullable = false)
    private Long viewCount;

    @Builder
    private ProductScore5Min(Long productId, LocalDateTime startTime, LocalDateTime endTime,
                             BigDecimal orderAmountSum, Long likeCount, Long viewCount) {
        this.productId = productId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.orderAmountSum = orderAmountSum != null ? orderAmountSum : BigDecimal.ZERO;
        this.likeCount = likeCount != null ? likeCount : 0L;
        this.viewCount = viewCount != null ? viewCount : 0L;
        this.guard();
    }

    @Override
    protected void guard() {
        if (productId == null) {
            throw new IllegalArgumentException("productId는 필수입니다");
        }
        if (startTime == null) {
            throw new IllegalArgumentException("startTime은 필수입니다");
        }
        if (endTime == null) {
            throw new IllegalArgumentException("endTime은 필수입니다");
        }
        if (startTime.isAfter(endTime) || startTime.equals(endTime)) {
            throw new IllegalArgumentException("startTime은 endTime보다 이전이어야 합니다");
        }
    }
}

