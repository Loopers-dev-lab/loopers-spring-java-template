package com.loopers.domain.stock.event;

import com.loopers.domain.order.saga.event.OrderCreatedEvent;

public record StockProcessedEvent(
    Long orderId,
    OrderCreatedEvent originalEvent
) {
}
