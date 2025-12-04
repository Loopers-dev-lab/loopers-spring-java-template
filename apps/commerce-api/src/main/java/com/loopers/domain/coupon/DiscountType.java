package com.loopers.domain.coupon;

import com.loopers.domain.Money;

public enum DiscountType {
    RATE(new RateDiscountCalculator()),
    AMOUNT(new AmountDiscountCalculator());

    private final DiscountCalculator calculator;

    DiscountType(DiscountCalculator calculator) {
        this.calculator = calculator;
    }

    public Money calculate(Money originalPrice, int discountValue) {
        return calculator.calculate(originalPrice, discountValue);
    }
}
