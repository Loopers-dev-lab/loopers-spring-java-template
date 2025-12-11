package com.loopers.domain.coupon.event;

public interface CouponEventPublisher {
    void publishCouponProcessed(CouponEvents.Processed event);
    void publishCouponProcessingFailed(CouponEvents.ProcessingFailed event);
    void publishCouponCompensated(CouponEvents.Compensated event);
}
