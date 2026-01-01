package com.loopers.domain.metrics;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Entity
@Table(
    name = "product_metrics",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"product_id", "metrics_date"})
    }
)
@Getter
public class ProductMetrics extends BaseEntity {

    @Column(nullable = false)
    private Long productId;

    @Column(name = "metrics_date", nullable = false)
    private LocalDate metricsDate;

    @Column(nullable = false)
    private Long likeCount;

    @Column(nullable = false)
    private Long salesCount;

    @Column(nullable = false)
    private Long viewCount;

    @Version
    @Column(nullable = false)
    private Long version;

    @Builder
    private ProductMetrics(Long productId, LocalDate metricsDate, Long likeCount, Long salesCount, Long viewCount) {
        this.productId = productId;
        this.metricsDate = metricsDate;
        this.likeCount = likeCount;
        this.salesCount = salesCount;
        this.viewCount = viewCount;
    }

    public ProductMetrics() {
    }

    public static ProductMetrics create(Long productId, LocalDate metricsDate) {
        return ProductMetrics.builder()
                .productId(productId)
                .metricsDate(metricsDate)
                .likeCount(0L)
                .salesCount(0L)
                .viewCount(0L)
                .build();
    }
}


