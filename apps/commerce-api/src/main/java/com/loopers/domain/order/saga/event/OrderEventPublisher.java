package com.loopers.domain.order.saga.event;

public interface OrderEventPublisher {
    void publishOrderCreated(OrderCreatedEvent event);
}
