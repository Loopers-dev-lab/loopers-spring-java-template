package com.loopers.domain.metrics;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Builder;
import lombok.Getter;

@Entity
@Table(name = "product_metrics")
@Getter
public class ProductMetrics extends BaseEntity {

    @Column(nullable = false, unique = true)
    private Long productId;

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
    private ProductMetrics(Long productId, Long likeCount, Long salesCount, Long viewCount) {
        this.productId = productId;
        this.likeCount = likeCount;
        this.salesCount = salesCount;
        this.viewCount = viewCount;
    }

    public ProductMetrics() {
    }

    public static ProductMetrics create(Long productId) {
        return ProductMetrics.builder()
                .productId(productId)
                .likeCount(0L)
                .salesCount(0L)
                .viewCount(0L)
                .build();
    }

    public void incrementLikeCount() {
        this.likeCount++;
    }

    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void incrementSalesCount(Integer quantity) {
        if (quantity != null && quantity > 0) {
            this.salesCount += quantity;
        }
    }
}

