package com.loopers.domain.coupon;

import com.loopers.domain.Money;
import com.loopers.domain.coupon.policy.AmountDiscountPolicy;
import com.loopers.domain.coupon.policy.DiscountPolicy;
import com.loopers.domain.coupon.policy.RateDiscountPolicy;

public enum DiscountType {
    RATE(new RateDiscountPolicy()),
    AMOUNT(new AmountDiscountPolicy());

    private final DiscountPolicy calculator;

    DiscountType(DiscountPolicy calculator) {
        this.calculator = calculator;
    }

    public Money calculate(Money originalPrice, int discountValue) {
        return calculator.calculate(originalPrice, discountValue);
    }
}
