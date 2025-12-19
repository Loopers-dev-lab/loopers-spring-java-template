package com.loopers.domain.metrics;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@Table(name = "product_metrics")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductMetrics {

    @EmbeddedId
    private ProductMetricsId id;

    @Column(name = "likes_delta")
    private int likesDelta;

    @Column(name = "sales_delta")
    private int salesDelta;

    @Column(name = "views_delta")
    private int viewsDelta;

    public static ProductMetrics create(Long productId, LocalDate date) {
        ProductMetrics productMetrics = new ProductMetrics();
        productMetrics.id = ProductMetricsId.create(productId, date);
        productMetrics.likesDelta = 0;
        productMetrics.salesDelta = 0;
        productMetrics.viewsDelta = 0;
        return productMetrics;
    }

    public void incrementLikes() {
        this.likesDelta++;
    }

    public void decrementLikes() {
        this.likesDelta--;
    }

    public void incrementSales(int quantity) {
        this.salesDelta += quantity;
    }

    public void decrementSales(int quantity) {
        this.salesDelta -= quantity;
    }

    public void incrementViews() {
        this.viewsDelta++;
    }
}
