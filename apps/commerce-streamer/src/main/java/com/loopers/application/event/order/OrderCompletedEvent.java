package com.loopers.application.event.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderCompletedEvent(
    String orderNo,
    Long memberId,
    BigDecimal totalPrice,
    List<OrderItemInfo> items,
    LocalDateTime completedAt
) {
    public record OrderItemInfo(
        Long productId,
        int quantity,
        BigDecimal price
    ) {}
}
