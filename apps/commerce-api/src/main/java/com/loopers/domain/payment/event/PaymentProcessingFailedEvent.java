package com.loopers.domain.payment.event;

public record PaymentProcessingFailedEvent(
    Long orderId,
    String reason
) {
}