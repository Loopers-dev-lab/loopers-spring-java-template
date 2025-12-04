package com.loopers.domain.coupon;

import com.loopers.domain.Money;

public interface DiscountCalculator {
    /**
     * 할인 금액을 계산
     * @param originalPrice 원래 금액
     * @param discountValue 할인 값 (정률의 경우 %, 정액의 경우 금액)
     * @return 할인 금액
     */
    Money calculate(Money originalPrice, int discountValue);
}