package com.loopers.domain.stock.event;

import com.loopers.domain.order.event.OrderEvents;

import java.util.List;

public class StockEvents {
    
    public record OrderItemInfo(
        Long productId,
        Integer quantity
    ) {}
    
    public record Processed(
        Long orderId,
        List<OrderItemInfo> orderItems,  // 재고 원복을 위해 필요
        OrderEvents.Created originalEvent
    ) {}
    
    public record ProcessingFailed(
        Long orderId,
        List<OrderItemInfo> orderItems,  // 재고 원복을 위해 필요
        String reason
    ) {}
    
    public record Compensated(
        Long orderId
    ) {}
}

