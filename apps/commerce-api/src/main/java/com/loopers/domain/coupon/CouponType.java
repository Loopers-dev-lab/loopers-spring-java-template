package com.loopers.domain.coupon;

import lombok.Getter;

@Getter
public enum CouponType {
    TEN(10),
    TWENTY(20),
    THIRTY(30),
    FORTY(40);

    private final int rate;

    CouponType(int rate) {
        this.rate = rate;
    }

}
