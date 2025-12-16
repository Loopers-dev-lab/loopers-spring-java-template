package com.loopers.domain.metrics;

import java.time.ZonedDateTime;

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

    @Column(name = "last_event_at")
    private ZonedDateTime lastEventAt;


    private ProductMetricsEntity(final Long productId) {
        this.id = productId;
    }

    public static ProductMetricsEntity create(final Long productId) {
        return new ProductMetricsEntity(productId);
    }

    public void incrementView() {
        this.viewCount += 1;
        this.lastEventAt = ZonedDateTime.now();
    }

    public void applyLikeDelta(final int delta) {
        final long next = this.likeCount + delta;

        //TODO : 검토 필요
        this.likeCount = Math.max(0, next);

        this.lastEventAt = ZonedDateTime.now();
    }

    public void addSales(final int quantity) {
        if (quantity <= 0) {
            return;
        }
        this.salesCount += quantity;
        this.lastEventAt = ZonedDateTime.now();
    }

}
