package com.loopers.domain.metrics.product;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "tb_product_metrics")
@Getter
public class ProductMetrics extends BaseEntity {
    // 현재는 상품의 좋아요 수만 관리하지만, 추후에 다른 메트릭들도 추가될 수 있습니다.
    private Long productId;
    private Integer likeCount;

    protected ProductMetrics() {
    }

    public static ProductMetrics create(Long productId, Integer likeCount) {
        if (productId == null || productId <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 ID는 1 이상이어야 합니다.");
        }
        if (likeCount == null || likeCount < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "좋아요 수는 0 이상이어야 합니다.");
        }
        ProductMetrics metrics = new ProductMetrics();
        metrics.productId = productId;
        metrics.likeCount = likeCount;
        return metrics;
    }
}
