package com.loopers.messaging.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderPaidEvent(
        Long orderId,
        List<OrderItemData> items,
        Instant occurredAt
) {
    public static OrderPaidEvent of(Long orderId, List<OrderItemData> items) {
        return new OrderPaidEvent(
                orderId,
                items,
                Instant.now()
        );
    }

    public record OrderItemData(
            Long productId,
            int quantity,
            BigDecimal unitPrice // 상품 단가
    ) {}
}
