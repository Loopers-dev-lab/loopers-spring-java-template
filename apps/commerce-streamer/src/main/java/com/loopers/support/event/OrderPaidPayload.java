package com.loopers.support.event;

import java.util.List;

public record OrderPaidPayload(
        List<OrderItem> items
) {
    public record OrderItem(
        Long productId,
        int quantity,
        boolean soldOut
    ) {}
}
