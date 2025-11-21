package com.loopers.domain.coupon;

public interface DiscountPolicy {

    int calculateDiscountAmount(int originalTotalPrice);

}

