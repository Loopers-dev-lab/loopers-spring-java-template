package com.loopers.domain.payment.event;

import java.math.BigDecimal;

/**
 * @author hyunjikoh
 * @since 2025. 12. 4.
 */
public record PaymentCompletedEvent(
        String transactionKey,
        Long orderId,
        Long userId,
        BigDecimal amount
) {
}
