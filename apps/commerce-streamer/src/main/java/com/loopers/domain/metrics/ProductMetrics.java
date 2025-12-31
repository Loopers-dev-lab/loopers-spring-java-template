package com.loopers.domain.metrics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "product_metrics")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ProductMetrics {

    @Id
    @Column(name = "product_id", nullable = false, updatable = false)
    private Long productId;

    @Column(name = "like_count", nullable = false)
    private Long likeCount;

    @Column(name = "view_count", nullable = false)
    private Long viewCount;

    @Column(name = "order_count", nullable = false)
    private Long orderCount;

    @Column(name = "sales_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal salesAmount;

    public static ProductMetrics create(Long productId) {
        return ProductMetrics.builder()
                .productId(productId)
                .likeCount(0L)
                .viewCount(0L)
                .orderCount(0L)
                .salesAmount(BigDecimal.ZERO)
                .build();
    }

    public void incrementLikeCount() {
        this.likeCount = (this.likeCount == null ? 1L : this.likeCount + 1L);
    }

    public void decrementLikeCount() {
        long current = this.likeCount == null ? 0L : this.likeCount;
        this.likeCount = Math.max(0L, current - 1L);
    }

    public void incrementViewCount() {
        this.viewCount = (this.viewCount == null ? 1L : this.viewCount + 1L);
    }

    public void incrementOrderCount(int quantity, BigDecimal amount) {
        long q = Math.max(0, quantity);
        this.orderCount = (this.orderCount == null ? q : this.orderCount + q);
        BigDecimal amt = amount == null ? BigDecimal.ZERO : amount;
        this.salesAmount = (this.salesAmount == null ? amt : this.salesAmount.add(amt));
    }
}



