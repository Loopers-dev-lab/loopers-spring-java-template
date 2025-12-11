package com.loopers.domain.coupon.event;

import com.loopers.shared.event.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * EventPublisher 구현체만 교체하면 됨
 */
@Component
@RequiredArgsConstructor
public class CouponEventPublisherImpl implements CouponEventPublisher {

    private final EventPublisher eventPublisher;
    
    private static final String TOPIC_COUPON_PROCESSED = "coupon.applied.v1";
    private static final String TOPIC_COUPON_FAILED = "coupon.apply-failed.v1";
    private static final String TOPIC_COUPON_COMPENSATED = "coupon.compensated.v1";

    @Override
    public void publishCouponProcessed(CouponEvents.Processed event) {
        eventPublisher.publish(TOPIC_COUPON_PROCESSED, String.valueOf(event.orderId()), event);
    }

    @Override
    public void publishCouponProcessingFailed(CouponEvents.ProcessingFailed event) {
        eventPublisher.publish(TOPIC_COUPON_FAILED, String.valueOf(event.orderId()), event);
    }

    @Override
    public void publishCouponCompensated(CouponEvents.Compensated event) {
        eventPublisher.publish(TOPIC_COUPON_COMPENSATED, String.valueOf(event.orderId()), event);
    }
}
