package com.loopers.domain.payment.event;

/**
 * @author hyunjikoh
 * @since 2025. 12. 4.
 */
public record PaymentFailedEvent(
        String transactionKey,
        Long orderId,
        Long userId,
        String reason
) {
}
