package com.loopers.domain.order.event;

public interface OrderEventPublisher {
    void publishOrderCreated(OrderEvents.Created event);
}
