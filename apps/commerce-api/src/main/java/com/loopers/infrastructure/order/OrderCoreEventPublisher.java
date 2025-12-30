package com.loopers.infrastructure.order;

import com.loopers.domain.order.event.OrderEvent;
import com.loopers.domain.order.event.OrderEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderCoreEventPublisher implements OrderEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(OrderEvent.PaymentRequested event) {
        applicationEventPublisher.publishEvent(event);
    }
}

