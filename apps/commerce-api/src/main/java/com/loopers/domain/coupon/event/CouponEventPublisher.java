package com.loopers.domain.coupon.event;

public interface CouponEventPublisher {
    void publishCouponProcessed(CouponProcessedEvent event);
    void publishCouponProcessingFailed(CouponProcessingFailedEvent event);
    void publishCouponCompensated(CouponCompensatedEvent event);
}
