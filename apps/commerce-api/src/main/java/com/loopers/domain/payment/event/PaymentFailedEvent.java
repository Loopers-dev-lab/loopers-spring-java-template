package com.loopers.domain.payment.event;

public record PaymentFailedEvent(Long orderId, Long userId, Long couponId, String reason) {
}
