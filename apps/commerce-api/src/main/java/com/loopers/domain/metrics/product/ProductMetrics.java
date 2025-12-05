package com.loopers.domain.metrics.product;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(
        name = "tb_product_metrics",
        indexes = {
                @Index(name = "idx_product_metrics_like_count", columnList = "like_count"),
                @Index(name = "idx_product_metrics_brand_id", columnList = "brand_id"),
                @Index(name = "idx_product_metrics_brand_like_count", columnList = "brand_id,like_count")
        }
)
@Getter
public class ProductMetrics extends BaseEntity {
    private Long productId;
    private Long brandId;
    private Integer likeCount;

    protected ProductMetrics() {
    }

    public static ProductMetrics create(Long productId, Long brandId, Integer likeCount) {
        if (productId == null || productId <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 ID는 1 이상이어야 합니다.");
        }
        if (brandId == null || brandId <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "브랜드 ID는 1 이상이어야 합니다.");
        }
        if (likeCount == null || likeCount < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "좋아요 수는 0 이상이어야 합니다.");
        }
        ProductMetrics metrics = new ProductMetrics();
        metrics.productId = productId;
        metrics.brandId = brandId;
        metrics.likeCount = likeCount;
        return metrics;
    }

    public void incrementLikeCount() {
        this.likeCount += 1;
    }

    public void decrementLikeCount() {
        if (this.likeCount <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "좋아요 수는 0 미만으로 내려갈 수 없습니다.");
        }
        this.likeCount -= 1;
    }
}
