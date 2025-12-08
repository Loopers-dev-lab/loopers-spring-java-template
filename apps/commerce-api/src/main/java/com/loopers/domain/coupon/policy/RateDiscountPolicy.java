package com.loopers.domain.coupon.policy;

import com.loopers.domain.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class RateDiscountPolicy implements DiscountPolicy {
    @Override
    public Money calculate(Money originalPrice, int discountValue) {
        // 정률 할인: 원래 금액 * (할인율 / 100)
        BigDecimal discountRate = BigDecimal.valueOf(discountValue)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal discountAmount = originalPrice.getAmount().multiply(discountRate);
        return Money.of(discountAmount);
    }
}
