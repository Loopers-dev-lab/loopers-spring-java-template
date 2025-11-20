package com.loopers.domain.coupon.discount;

import org.springframework.stereotype.Component;

/**
 * 정률 쿠폰 할인 계산 전략.
 * <p>
 * 주문 금액의 일정 비율을 할인합니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@Component
public class PercentageDiscountStrategy implements CouponDiscountStrategy {
    /**
     * Calculate the discount amount by applying a percentage of the order amount.
     *
     * @param orderAmount   the order amount
     * @param discountValue the discount rate as a percentage (0-100)
     * @return the discount amount rounded to the nearest integer
     */
    @Override
    public Integer calculateDiscountAmount(Integer orderAmount, Integer discountValue) {
        // 주문 금액의 할인 비율만큼 할인
        return (int) Math.round(orderAmount * discountValue / 100.0);
    }
}
