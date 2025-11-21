package com.loopers.domain.coupon;

import com.loopers.domain.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum DiscountType {
    RATE {
        @Override
        public Money calculate(Money originalPrice, int discountValue) {
            // 정률 할인: 원래 금액 * (할인율 / 100)
            BigDecimal discountRate = BigDecimal.valueOf(discountValue)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal discountAmount = originalPrice.getAmount().multiply(discountRate);
            return Money.of(discountAmount);
        }
    },
    AMOUNT {
        @Override
        public Money calculate(Money originalPrice, int discountValue) {
            // 정액 할인: 고정 금액
            return Money.of(discountValue);
        }
    };

    /**
     * 할인 금액을 계산
     * @param originalPrice 원래 금액
     * @param discountValue 할인 값 (정률의 경우 %, 정액의 경우 금액)
     * @return 할인 금액
     */
    public abstract Money calculate(Money originalPrice, int discountValue);
}
