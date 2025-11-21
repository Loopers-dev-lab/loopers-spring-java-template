package com.loopers.domain.coupon;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author hyunjikoh
 * @since 2025. 11. 18.
 */
@Getter
@RequiredArgsConstructor
public enum CouponType {
    FIXED_AMOUNT("정액 할인"),
    PERCENTAGE("배율 할인");

    private final String description;
}
