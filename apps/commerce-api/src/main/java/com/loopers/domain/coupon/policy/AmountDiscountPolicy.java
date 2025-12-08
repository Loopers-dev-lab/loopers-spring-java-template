package com.loopers.domain.coupon.policy;

import com.loopers.domain.Money;

public class AmountDiscountPolicy implements DiscountPolicy {
    @Override
    public Money calculate(Money originalPrice, int discountValue) {
        // 정액 할인: 고정 금액
        Money discount = Money.of(discountValue);
        // 할인 금액이 원가를 초과하지 않도록 제한
        return originalPrice.isLessThan(discount) ? originalPrice : discount;
    }
}
