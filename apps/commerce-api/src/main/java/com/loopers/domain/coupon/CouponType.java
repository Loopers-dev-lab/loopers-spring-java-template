package com.loopers.domain.coupon;

/**
 * 쿠폰 타입.
 * <p>
 * 정액 쿠폰과 정률 쿠폰을 구분합니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
public enum CouponType {
    /**
     * 정액 쿠폰: 고정 금액 할인
     */
    FIXED_AMOUNT,

    /**
     * 정률 쿠폰: 비율 할인
     */
    PERCENTAGE
}

