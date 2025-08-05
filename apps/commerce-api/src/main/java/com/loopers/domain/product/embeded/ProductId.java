package com.loopers.domain.product.embeded;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Embeddable;

@Embeddable
public class ProductId {
    private Long productId;

    private ProductId(Long productId) {
        this.productId = productId;
    }

    public ProductId() {
    }

    public static ProductId of(Long productId) {
        if (productId == null || productId <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "productId는 양수여야 합니다.");
        }
        return new ProductId(productId);
    }

    public Long getValue() {
        return this.productId;
    }
}