package com.loopers.domain.order;

public interface OrderCreatedEventHandler {
    void handleOrderCreated(OrderEvent.OrderCreatedEvent event);
}

