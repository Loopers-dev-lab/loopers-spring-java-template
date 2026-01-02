package com.loopers.batch.domain.metrics;

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

    public Long getProductId() {
        return id.getProductId();
    }

    public LocalDate getMetricsDate() {
        return id.getMetricsDate();
    }
}
