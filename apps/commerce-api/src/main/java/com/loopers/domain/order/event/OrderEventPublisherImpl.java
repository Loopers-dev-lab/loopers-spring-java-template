package com.loopers.domain.order.event;

import com.loopers.shared.event.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderEventPublisherImpl implements OrderEventPublisher {

    private final EventPublisher eventPublisher;

    @Override
    public void publishOrderCreated(OrderEvents.Created event) {
        eventPublisher.publish(event);
    }
    
    @Override
    public void publishOrderConfirmed(OrderEvents.Confirmed event) {
        eventPublisher.publish(event);
    }
}
