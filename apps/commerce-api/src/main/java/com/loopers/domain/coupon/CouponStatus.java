package com.loopers.domain.coupon;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author hyunjikoh
 * @since 2025. 11. 18.
 */
@Getter
@RequiredArgsConstructor
public enum CouponStatus {
    UNUSED("미사용"),
    USED("사용");

    private final String description;
}
