package com.loopers.support.event;

import java.math.BigDecimal;
import java.util.List;

public record OrderPaidPayload(
        List<OrderItem> items
) {
    public record OrderItem(
        Long productId,
        int quantity,
        BigDecimal totalPrice,
        boolean soldOut
    ) {}
}
