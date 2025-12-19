package com.loopers.domain.order;

public interface OrderEventPublisher {
    void publishOrderCreated(OrderEvent.OrderCreatedEvent event);
    void publishOrderPaid(OrderEvent.OrderPaidEvent event);
}
