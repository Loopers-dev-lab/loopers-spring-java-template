package com.loopers.application.order;

import java.util.List;

public record OrderPlaceCommand(
        String userId,
        List<OrderItemCommand> items,
        Long couponId
) {
    public record OrderItemCommand(
            Long productId,
            Integer quantity
    ) {}
}
