package com.loopers.domain.payment.event;

/**
 * @author hyunjikoh
 * @since 2025. 12. 4.
 */
public record PaymentTimeoutEvent(
        String transactionKey,
        Long orderId,
        Long userId
) {
}
