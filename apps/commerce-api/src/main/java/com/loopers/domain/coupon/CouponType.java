package com.loopers.domain.coupon;

import java.math.BigDecimal;
import java.math.RoundingMode;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

public enum CouponType {
    FIXED_AMOUNT {  // 정가 할인
        @Override
        public BigDecimal calculateDiscount(BigDecimal discountValue, BigDecimal totalPrice) {
            return discountValue.min(totalPrice); // 할인액이 총액보다 클 수 없음
        }
    },
    PERCENTAGE {    // 할인율
        @Override
        public BigDecimal calculateDiscount(BigDecimal discountValue, BigDecimal totalPrice) {
            // 할인율 검증
            if (discountValue.compareTo(BigDecimal.ZERO) < 0 || discountValue.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new CoreException(
                        ErrorType.BAD_REQUEST,
                        "Coupon : 할인율은 0~100 사이의 값이어야 합니다."
                );
            }
            return totalPrice.multiply(discountValue)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
    };

    public abstract BigDecimal calculateDiscount(BigDecimal discountValue, BigDecimal totalPrice);
}

