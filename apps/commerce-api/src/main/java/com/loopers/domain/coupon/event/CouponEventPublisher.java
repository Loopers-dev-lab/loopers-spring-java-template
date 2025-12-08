package com.loopers.domain.coupon.event;

public interface CouponEventPublisher {
    void publishCouponProcess(CouponProcessEvent event);
}
