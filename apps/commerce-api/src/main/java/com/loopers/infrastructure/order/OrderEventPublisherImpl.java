package com.loopers.infrastructure.order;

import com.loopers.domain.order.OrderEvent;
import com.loopers.domain.order.OrderEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderEventPublisherImpl implements OrderEventPublisher {
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publishOrderCreated(OrderEvent.OrderCreatedEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    public void publishOrderPaid(OrderEvent.OrderPaidEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}
