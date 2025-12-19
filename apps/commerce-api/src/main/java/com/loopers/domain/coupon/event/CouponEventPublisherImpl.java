package com.loopers.domain.coupon.event;

import com.loopers.shared.event.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CouponEventPublisherImpl implements CouponEventPublisher {

    private final EventPublisher eventPublisher;

    @Override
    public void publishCouponProcessed(CouponEvents.Processed event) {
        eventPublisher.publish(event);
    }

    @Override
    public void publishCouponProcessingFailed(CouponEvents.ProcessingFailed event) {
        eventPublisher.publish(event);
    }

    @Override
    public void publishCouponCompensated(CouponEvents.Compensated event) {
        eventPublisher.publish(event);
    }
}
