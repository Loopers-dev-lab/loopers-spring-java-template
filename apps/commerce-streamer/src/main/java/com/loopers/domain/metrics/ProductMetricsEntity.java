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

}
