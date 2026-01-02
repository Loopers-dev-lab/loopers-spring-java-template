package com.loopers.domain.metrics;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.ZonedDateTime;

@Entity
@Table(
        name = "product_metrics_daily",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_product_date",
                        columnNames = {"product_id", "metric_date"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_metric_date_processed",
                        columnList = "metric_date, is_processed"
                )
        }
)
@NoArgsConstructor
@Getter
public class ProductMetricsDaily extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;

    @Column(name = "like_delta", nullable = false)
    private Integer likeDelta = 0;

    @Column(name = "view_delta", nullable = false)
    private Integer viewDelta = 0;

    @Column(name = "order_delta", nullable = false)
    private Integer orderDelta = 0;

    @Column(name = "is_processed", nullable = false)
    private Boolean isProcessed = false;

    @Column(name = "processed_at")
    private ZonedDateTime processedAt;

    public static ProductMetricsDaily create(Long productId, LocalDate metricDate) {
        ProductMetricsDaily daily = new ProductMetricsDaily();
        daily.productId = productId;
        daily.metricDate = metricDate;
        return daily;
    }

    public void addLikeDelta(int delta) {
        this.likeDelta += delta;
    }

    public void addViewDelta(int delta) {
        this.viewDelta += delta;
    }

    public void addOrderDelta(int delta) {
        this.orderDelta += delta;
    }

    public void markAsProcessed() {
        this.isProcessed = true;
        this.processedAt = ZonedDateTime.now();
    }
}
