package com.loopers.infrastructure.metrics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_metrics", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"productId", "date"})
})
public class ProductMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private LocalDate date;

    private Long viewCount;
    private Long likeCount;
    private Long orderCount;
    private Long totalQuantity;
    private LocalDateTime lastLikeEventAt;

    protected ProductMetrics() {
    }

    private ProductMetrics(Long productId, LocalDate date) {
        this.productId = productId;
        this.date = date;
        this.viewCount = 0L;
        this.likeCount = 0L;
        this.orderCount = 0L;
        this.totalQuantity = 0L;
    }

    public static ProductMetrics create(Long productId) {
        return new ProductMetrics(productId, LocalDate.now());
    }

    public static ProductMetrics create(Long productId, LocalDate date) {
        return new ProductMetrics(productId, date);
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void incrementLikeCount() {
        this.likeCount++;
    }

    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    public void addOrder(int quantity) {
        this.orderCount++;
        this.totalQuantity += quantity;
    }

    public boolean updateLikeIfNewer(boolean liked, LocalDateTime eventTime) {
        if (this.lastLikeEventAt != null && !eventTime.isAfter(this.lastLikeEventAt)) {
            return false;
        }
        if (liked) {
            incrementLikeCount();
        } else {
            decrementLikeCount();
        }
        this.lastLikeEventAt = eventTime;
        return true;
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public LocalDate getDate() {
        return date;
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

    public LocalDateTime getLastLikeEventAt() {
        return lastLikeEventAt;
    }
}
