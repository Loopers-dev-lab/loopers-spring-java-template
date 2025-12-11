package com.loopers.domain.order.event;

import com.loopers.interfaces.api.order.OrderDto;

public class OrderEvents {
    
    public record Created(
        Long userId,
        Long orderId,
        OrderDto.CreateOrderRequest request
    ) {}
}

