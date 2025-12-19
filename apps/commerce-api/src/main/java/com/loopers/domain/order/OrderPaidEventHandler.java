package com.loopers.domain.order;

public interface OrderPaidEventHandler {
    void handleOrderPaid(OrderEvent.OrderPaidEvent event);
}

