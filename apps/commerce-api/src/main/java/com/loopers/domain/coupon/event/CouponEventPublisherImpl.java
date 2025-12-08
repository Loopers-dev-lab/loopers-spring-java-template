package com.loopers.domain.coupon.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CouponEventPublisherImpl implements CouponEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void publishCouponProcess(CouponProcessEvent event) {
        eventPublisher.publishEvent(event);
    }
}
