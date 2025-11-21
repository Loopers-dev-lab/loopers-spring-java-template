package com.loopers.domain.coupon.discount;

import org.springframework.stereotype.Component;

/**
 * 정액 쿠폰 할인 계산 전략.
 * <p>
 * 고정 금액을 할인하며, 할인 금액이 주문 금액을 초과하지 않도록 보장합니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@Component
public class FixedAmountDiscountStrategy implements CouponDiscountStrategy {
    /**
     * {@inheritDoc}
     * <p>
     * 정액 쿠폰: 할인 금액이 주문 금액을 초과하지 않도록 제한합니다.
     * </p>
     *
     * @param orderAmount 주문 금액
     * @param discountValue 할인 금액
     * @return 할인 금액 (주문 금액을 초과하지 않음)
     */
    @Override
    public Integer calculateDiscountAmount(Integer orderAmount, Integer discountValue) {
        // 할인 금액이 주문 금액을 초과하지 않도록
        return Math.min(discountValue, orderAmount);
    }
}

