package com.loopers.domain.product.embeded;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
@Embeddable
public class ProductLikeCount {
    private BigDecimal productLikeCount;

    private ProductLikeCount(BigDecimal productLikeCount) {
        this.productLikeCount = productLikeCount;
    }

    public ProductLikeCount() {

    }

    public static ProductLikeCount of(BigDecimal productLikeCount) {
        if(productLikeCount.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "productLikeCount는 0보다 커야 합니다.");
        }
        return new ProductLikeCount(productLikeCount);
    }
    public ProductLikeCount increment() {
        return new ProductLikeCount(this.productLikeCount.add(BigDecimal.ONE));
    }
    
    public ProductLikeCount decrement() {
        if (this.productLikeCount.compareTo(BigDecimal.ZERO) <= 0) {
            return new ProductLikeCount(this.productLikeCount);
        }
        return new ProductLikeCount(this.productLikeCount.subtract(BigDecimal.ONE));
    }

    public BigDecimal getValue() {
        return this.productLikeCount;
    }
}
