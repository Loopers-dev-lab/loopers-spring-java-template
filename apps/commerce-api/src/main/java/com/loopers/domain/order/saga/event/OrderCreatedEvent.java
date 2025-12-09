package com.loopers.domain.order.saga.event;

import com.loopers.interfaces.api.order.OrderDto;

public record OrderCreatedEvent(
    Long userId,
    Long orderId,
    OrderDto.CreateOrderRequest request
) {
}
