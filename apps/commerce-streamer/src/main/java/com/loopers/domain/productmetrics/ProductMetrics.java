package com.loopers.domain.productmetrics;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.ZonedDateTime;

@Getter
@Entity
@Table(name = "product_metrics",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"product_id", "metric_date"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductMetrics {

    @Id
    @GeneratedValue
    private Long id;

    private Long productId;

    private LocalDate metricDate;

    @Column(nullable = false)
    private int likeCount;

    @Column(nullable = false)
    private long salesCount;

    @Column(nullable = false)
    private long salesAmount;

    @Column(nullable = false)
    private long viewCount;

    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;

    public ProductMetrics(Long productId) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("상품 ID는 0보다 커야 합니다.");
        }
        this.productId = productId;
        this.likeCount = 0;
        this.salesCount = 0;
        this.salesAmount = 0;
        this.viewCount = 0;
        this.createdAt = ZonedDateTime.now();
        this.updatedAt = ZonedDateTime.now();
    }

    public static ProductMetrics create(Long productId) {
        return new ProductMetrics(productId);
    }

}
