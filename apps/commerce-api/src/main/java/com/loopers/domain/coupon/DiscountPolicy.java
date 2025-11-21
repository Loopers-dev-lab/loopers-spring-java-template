package com.loopers.domain.coupon;

import com.loopers.domain.Money;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@EqualsAndHashCode
public class DiscountPolicy {
    private DiscountType discountType;
    private int discountValue;

    @Builder
    public DiscountPolicy(DiscountType discountType, int discountValue) {
        this.discountType = discountType;
        this.discountValue = discountValue;
    }

    public Money calculateDiscount(Money originalPrice) {
        return discountType.calculate(originalPrice, discountValue);
    }

    public Money applyDiscount(Money originalPrice) {
        Money discountAmount = calculateDiscount(originalPrice);

        // 할인 금액이 원래 금액보다 크면 0원 반환
        if (discountAmount.isGreaterThanOrEqual(originalPrice)) {
            return Money.zero();
        }

        return originalPrice.subtract(discountAmount);
    }
}
