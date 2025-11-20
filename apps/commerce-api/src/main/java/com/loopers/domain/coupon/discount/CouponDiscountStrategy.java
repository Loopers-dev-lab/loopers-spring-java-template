package com.loopers.domain.coupon.discount;

/**
 * 쿠폰 할인 계산 전략 인터페이스.
 * <p>
 * 전략 패턴을 사용하여 쿠폰 타입별 할인 계산 로직을 분리합니다.
 * 새로운 쿠폰 타입이 추가되어도 기존 코드를 수정하지 않고 확장할 수 있습니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
public interface CouponDiscountStrategy {
    /**
 * Calculate the discount amount to apply to a given order.
 *
 * @param orderAmount the total order amount
 * @param discountValue the coupon's discount value; its interpretation depends on the coupon type
 * @return the calculated discount amount
 */
    Integer calculateDiscountAmount(Integer orderAmount, Integer discountValue);
}
