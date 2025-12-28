package com.loopers.application.event;

import com.loopers.domain.order.Money;
import com.loopers.domain.order.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderDataTransferEvent(
    String orderId,
    Long userId,
    OrderStatus status,
    Money totalAmount,
    LocalDateTime completedAt,
    String eventType // "ORDER_CREATED", "ORDER_PAID", "ORDER_CANCELLED"
) {
}
