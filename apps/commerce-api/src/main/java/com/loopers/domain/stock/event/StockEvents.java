package com.loopers.domain.stock.event;

import com.loopers.domain.order.event.OrderEvents;

public class StockEvents {
    
    public record Processed(
        Long orderId,
        OrderEvents.Created originalEvent
    ) {}
    
    public record ProcessingFailed(
        Long orderId,
        String reason
    ) {}
    
    public record Compensated(
        Long orderId
    ) {}
}

