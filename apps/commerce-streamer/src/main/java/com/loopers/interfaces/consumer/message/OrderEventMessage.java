package com.loopers.interfaces.consumer.message;

import java.time.ZonedDateTime;
import java.util.List;

public record OrderEventMessage(
        String eventId,
        String eventType,
        Long orderId,
        String userId,
        Long totalAmount,
        String orderStatus,
        String paymentStatus,
        String transactionKey,
        String reason,
        ZonedDateTime occurredAt,
        List<OrderItemPayload> items
) {
    public record OrderItemPayload(Long productId, String productName, Long quantity, Long price) {
    }
}
