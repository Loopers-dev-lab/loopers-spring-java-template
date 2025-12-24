package com.loopers.infrastructure.kafka.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderEventDto(
        String eventId,
        Long orderId,
        Long userId,
        String orderStatus,
        Long totalAmount,
        Long discountAmount,
        List<OrderItemDto> items,
        LocalDateTime occurredAt
) {
    public static OrderEventDto created(
            Long orderId,
            Long userId,
            Long totalAmount,
            Long discountAmount,
            List<OrderItemDto> items
    ) {
        return new OrderEventDto(
                UUID.randomUUID().toString(),
                orderId,
                userId,
                "CREATED",
                totalAmount,
                discountAmount,
                items,
                LocalDateTime.now()
        );
    }

    public static OrderEventDto completed(Long orderId, Long userId) {
        return new OrderEventDto(
                UUID.randomUUID().toString(),
                orderId,
                userId,
                "COMPLETED",
                null,
                null,
                null,
                LocalDateTime.now()
        );
    }

    public static OrderEventDto failed(Long orderId, Long userId) {
        return new OrderEventDto(
                UUID.randomUUID().toString(),
                orderId,
                userId,
                "FAILED",
                null,
                null,
                null,
                LocalDateTime.now()
        );
    }

    public record OrderItemDto(
            Long productId,
            int quantity,
            Long unitPrice
    ) {}
}
