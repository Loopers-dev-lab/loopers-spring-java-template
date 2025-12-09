package com.loopers.domain.coupon.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CouponEventPublisherImpl implements CouponEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void publishCouponProcessed(CouponProcessedEvent event) {
        eventPublisher.publishEvent(event);
    }

    @Override
    public void publishCouponProcessingFailed(CouponProcessingFailedEvent event) {
        eventPublisher.publishEvent(event);
    }

    @Override
    public void publishCouponCompensated(CouponCompensatedEvent event) {
        eventPublisher.publishEvent(event);
    }
}
