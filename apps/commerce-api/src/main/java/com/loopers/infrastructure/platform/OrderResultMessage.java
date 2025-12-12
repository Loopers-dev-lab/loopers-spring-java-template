package com.loopers.infrastructure.platform;

import java.time.ZonedDateTime;
import java.util.List;

public record OrderResultMessage(
        Long orderId,
        Long userId,
        OrderAction action,
        Long totalAmount,
        Long discountAmount,
        List<OrderItemInfo> items,
        ZonedDateTime occurredAt
) {
    public enum OrderAction {
        CREATED,
        COMPLETED,
        CANCELLED
    }

    public record OrderItemInfo(Long productId, Integer quantity, Long unitPrice) {
    }

    public static OrderResultMessage created(
            Long orderId, Long userId,
            Long totalAmount, Long discountAmount, List<OrderItemInfo> items
    ) {
        return new OrderResultMessage(
                orderId, userId,
                OrderAction.CREATED, totalAmount, discountAmount, items,
                ZonedDateTime.now()
        );
    }

    public static OrderResultMessage completed(Long orderId, Long userId) {
        return new OrderResultMessage(
                orderId, userId,
                OrderAction.COMPLETED, null, null, null,
                ZonedDateTime.now()
        );
    }

    public static OrderResultMessage cancelled(Long orderId, Long userId) {
        return new OrderResultMessage(
                orderId, userId,
                OrderAction.CANCELLED, null, null, null,
                ZonedDateTime.now()
        );
    }
}
