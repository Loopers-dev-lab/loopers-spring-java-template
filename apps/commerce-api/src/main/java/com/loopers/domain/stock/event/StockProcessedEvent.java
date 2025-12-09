package com.loopers.domain.stock.event;

import com.loopers.domain.order.event.OrderCreatedEvent;

public record StockProcessedEvent(
    Long orderId,
    OrderCreatedEvent originalEvent
) {
}