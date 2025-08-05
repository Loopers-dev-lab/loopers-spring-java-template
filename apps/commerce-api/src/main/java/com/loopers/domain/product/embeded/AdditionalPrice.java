package com.loopers.domain.product.embeded;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

@Embeddable
public class AdditionalPrice {
    private BigDecimal additionalPrice;

    private AdditionalPrice(BigDecimal additionalPrice) {
        this.additionalPrice = additionalPrice;
    }

    public AdditionalPrice() {
    }

    public static AdditionalPrice of(BigDecimal additionalPrice) {
        if (additionalPrice == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "추가 가격은 null일 수 없습니다.");
        }
        return new AdditionalPrice(additionalPrice);
    }

    public BigDecimal getValue() {
        return this.additionalPrice;
    }

    public boolean isPositive() {
        return this.additionalPrice.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isNegative() {
        return this.additionalPrice.compareTo(BigDecimal.ZERO) < 0;
    }

    public boolean isZero() {
        return this.additionalPrice.compareTo(BigDecimal.ZERO) == 0;
    }
}
