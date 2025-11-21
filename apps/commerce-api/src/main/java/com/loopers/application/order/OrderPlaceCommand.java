package com.loopers.application.order;

import java.util.List;

public record OrderPlaceCommand(
        String userId,
        List<OrderItemCommand> items
) {
    public record OrderItemCommand(
            Long productId,
            Integer quantity
    ) {}
}
