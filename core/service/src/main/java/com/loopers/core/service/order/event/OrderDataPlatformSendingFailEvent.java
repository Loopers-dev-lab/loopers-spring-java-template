package com.loopers.core.service.order.event;

import com.loopers.core.domain.order.vo.OrderId;

public record OrderDataPlatformSendingFailEvent(
        OrderId orderId,
        boolean retryable,
        int retryCount,
        String message
) {

    public static OrderDataPlatformSendingFailEvent create(
            OrderId orderId,
            boolean retryable,
            String message) {

        return new OrderDataPlatformSendingFailEvent(orderId, retryable, 0, message);
    }
}
