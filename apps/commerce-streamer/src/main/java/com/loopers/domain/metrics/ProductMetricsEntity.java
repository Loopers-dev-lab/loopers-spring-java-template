package com.loopers.domain.metrics;

import java.time.ZonedDateTime;
import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 *
 * @author hyunjikoh
 * @since 2025. 12. 16.
 */

@Entity
@Getter
@Table(name = "product_metrics")
@AllArgsConstructor
@NoArgsConstructor
public class ProductMetricsEntity {
    @Id
    @Column(name = "product_id", nullable = false)
    private Long id;

    @Column(name = "view_count", nullable = false)
    private long viewCount = 0L;

    @Column(name = "like_count", nullable = false)
    private long likeCount = 0L;

    @Column(name = "sales_count", nullable = false)
    private long salesCount = 0L;

    @Column(name = "order_count", nullable = false)
    private long orderCount = 0L;

    @Column(name = "cart_add_count", nullable = false)
    private long cartAddCount = 0L;

    @Column(name = "wishlist_add_count", nullable = false)
    private long wishlistAddCount = 0L;

    @Column(name = "review_count", nullable = false)
    private long reviewCount = 0L;

    @Column(name = "last_event_at")
    private ZonedDateTime lastEventAt;


    private ProductMetricsEntity(final Long productId) {
        Objects.requireNonNull(productId);
        this.id = productId;
    }

    public static ProductMetricsEntity create(final Long productId) {
        return new ProductMetricsEntity(productId);
    }

    
    public void incrementView(ZonedDateTime eventTime) {
        this.viewCount += 1;
        this.lastEventAt = eventTime;
    }

    public void applyLikeDelta(final int delta) {
        final long next = this.likeCount + delta;

        //TODO : 검토 필요
        this.likeCount = Math.max(0, next);

        this.lastEventAt = ZonedDateTime.now();
    }
    
    public void applyLikeDelta(final int delta, ZonedDateTime eventTime) {
        final long next = this.likeCount + delta;

        // 좋아요 수는 0 미만으로 내려가지 않도록 보장
        this.likeCount = Math.max(0, next);

        this.lastEventAt = eventTime;
    }


    public void addSales(final int quantity, ZonedDateTime eventTime) {
        if (quantity <= 0) {
            return;
        }
        this.salesCount += quantity;
        this.orderCount += 1; // 주문 건수도 함께 증가
        this.lastEventAt = eventTime;
    }

    /**
     * 장바구니 추가 이벤트 처리
     */
    public void incrementCartAdd(ZonedDateTime eventTime) {
        this.cartAddCount += 1;
        this.lastEventAt = eventTime;
    }

    /**
     * 위시리스트 추가 이벤트 처리
     */
    public void incrementWishlistAdd(ZonedDateTime eventTime) {
        this.wishlistAddCount += 1;
        this.lastEventAt = eventTime;
    }

    /**
     * 리뷰 작성 이벤트 처리
     */
    public void incrementReview(ZonedDateTime eventTime) {
        this.reviewCount += 1;
        this.lastEventAt = eventTime;
    }

    // ========== 비즈니스 분석 메서드 ==========

    /**
     * 인기도 점수 계산 (가중치 기반)
     * - 조회수: 1점
     * - 좋아요: 5점
     * - 장바구니 추가: 10점
     * - 위시리스트 추가: 8점
     * - 주문: 20점
     * - 리뷰: 15점
     */
    public double calculatePopularityScore() {
        return (viewCount * 1.0) +
               (likeCount * 5.0) +
               (cartAddCount * 10.0) +
               (wishlistAddCount * 8.0) +
               (orderCount * 20.0) +
               (reviewCount * 15.0);
    }

    /**
     * 전환율 계산 (주문 수 / 조회 수)
     */
    public double calculateConversionRate() {
        if (viewCount == 0) {
            return 0.0;
        }
        return (double) orderCount / viewCount * 100.0; // 백분율
    }

    /**
     * 평균 주문 수량 계산
     */
    public double calculateAverageOrderQuantity() {
        if (orderCount == 0) {
            return 0.0;
        }
        return (double) salesCount / orderCount;
    }

    /**
     * 참여도 점수 계산 (조회 대비 액션 비율)
     */
    public double calculateEngagementRate() {
        if (viewCount == 0) {
            return 0.0;
        }
        long totalEngagements = likeCount + cartAddCount + wishlistAddCount + orderCount;
        return (double) totalEngagements / viewCount * 100.0; // 백분율
    }

}
