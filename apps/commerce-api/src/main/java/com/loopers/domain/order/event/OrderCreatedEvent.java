package com.loopers.domain.order.event;

import com.loopers.domain.payment.PaymentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderCreatedEvent(
        Long orderId,
        Long userId,
        BigDecimal totalPrice,
        PaymentType paymentType,
        LocalDateTime createdAt
) {
    public static OrderCreatedEvent of(
            Long orderId,
            Long userId,
            BigDecimal totalPrice,
            PaymentType paymentType
    ) {
        return new OrderCreatedEvent(
                orderId,
                userId,
                totalPrice,
                paymentType,
                LocalDateTime.now()
        );
    }
}
