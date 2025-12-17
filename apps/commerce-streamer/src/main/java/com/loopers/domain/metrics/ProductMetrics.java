package com.loopers.domain.metrics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상품별 Metrics 집계 테이블
 * - 좋아요 수, 조회 수, 주문 수, 판매 금액 집계
 * - Optimistic Lock (version)으로 동시성 제어
 */
@Getter
@Entity
@Table(
    name = "product_metrics",
    indexes = {
        @Index(name = "idx_like_count", columnList = "like_count DESC"),
        @Index(name = "idx_view_count", columnList = "view_count DESC"),
        @Index(name = "idx_order_count", columnList = "order_count DESC"),
        @Index(name = "idx_updated_at", columnList = "updated_at")
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductMetrics {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "like_count", nullable = false)
    private Integer likeCount = 0;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;

    @Column(name = "order_count", nullable = false)
    private Integer orderCount = 0;

    @Column(name = "sales_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal salesAmount = BigDecimal.ZERO;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    @Builder
    private ProductMetrics(Long productId) {
        this.productId = productId;
        this.likeCount = 0;
        this.viewCount = 0;
        this.orderCount = 0;
        this.salesAmount = BigDecimal.ZERO;
        this.version = 0;
    }

    @PrePersist
    private void prePersist() {
        ZonedDateTime now = ZonedDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = ZonedDateTime.now();
    }

    /**
     * 좋아요 수 증가
     */
    public void incrementLikeCount() {
        this.likeCount++;
    }

    /**
     * 좋아요 수 감소
     */
    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    /**
     * 조회 수 증가
     */
    public void incrementViewCount() {
        this.viewCount++;
    }

    /**
     * 주문 수 및 판매 금액 증가
     */
    public void incrementOrderCount(int quantity, BigDecimal amount) {
        this.orderCount += quantity;
        this.salesAmount = this.salesAmount.add(amount);
    }
}
