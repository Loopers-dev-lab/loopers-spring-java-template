package com.loopers.domain.order.event;

public interface OrderEventPublisher {

    void publish(OrderEvent.PaymentRequested event);
}

