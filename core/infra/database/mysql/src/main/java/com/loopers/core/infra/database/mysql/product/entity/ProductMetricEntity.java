package com.loopers.core.infra.database.mysql.product.entity;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.product.ProductMetric;
import com.loopers.core.domain.product.vo.ProductDetailViewCount;
import com.loopers.core.domain.product.vo.ProductId;
import com.loopers.core.domain.product.vo.ProductMetricId;
import com.loopers.core.domain.product.vo.ProductTotalSalesCount;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Entity
@Table(
        name = "product_metrics",
        indexes = {
                @Index(name = "idx_product_metric_product_id", columnList = "product_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ProductMetricEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Long totalSalesCount;

    @Column(nullable = false)
    private Long viewCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static ProductMetricEntity from(ProductMetric metric) {
        return new ProductMetricEntity(
                Optional.ofNullable(metric.getId().value())
                        .map(Long::parseLong)
                        .orElse(null),
                Long.parseLong(Objects.requireNonNull(metric.getProductId().value())),
                metric.getTotalSalesCount().value(),
                metric.getViewCount().value(),
                metric.getCreatedAt().value(),
                metric.getUpdatedAt().value()
        );
    }

    public ProductMetric to() {
        return ProductMetric.mappedBy(
                new ProductMetricId(this.id.toString()),
                new ProductId(this.productId.toString()),
                new ProductTotalSalesCount(this.totalSalesCount),
                new ProductDetailViewCount(this.viewCount),
                new CreatedAt(this.createdAt),
                new UpdatedAt(this.updatedAt)
        );
    }
}
