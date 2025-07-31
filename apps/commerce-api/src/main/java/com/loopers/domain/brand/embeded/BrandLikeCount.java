package com.loopers.domain.brand.embeded;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Embeddable;
import lombok.Getter;

import java.math.BigDecimal;

@Embeddable
@Getter
public class BrandLikeCount {
    private BigDecimal brandLikeCount;

    private BrandLikeCount(BigDecimal brandLikeCount) {
        this.brandLikeCount = brandLikeCount;
    }

    public BrandLikeCount() {

    }

    public static BrandLikeCount of(BigDecimal likeCount) {
        if(likeCount != null && likeCount.compareTo(BigDecimal.ZERO) < 0){
            throw new CoreException(ErrorType.BAD_REQUEST, "brandLikeCount 0보다 커야 합니다.");
        }
        return new BrandLikeCount(likeCount);
    }
    public BrandLikeCount increment() {
        return new BrandLikeCount(this.brandLikeCount.add(BigDecimal.ONE));
    }

    public BrandLikeCount decrement() {
        if (this.brandLikeCount.compareTo(BigDecimal.ZERO) <= 0) {
            return new BrandLikeCount(this.brandLikeCount);
        }
        return new BrandLikeCount(this.brandLikeCount.subtract(BigDecimal.ONE));
    }
}
