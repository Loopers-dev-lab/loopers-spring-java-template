package com.loopers.domain.order.event;

import java.math.BigDecimal;

/**
 * 주문 확정 이벤트
 *
 * @author hyunjikoh
 * @since 2025. 12. 12.
 */
public record OrderConfirmedEvent(
        Long orderId,
        Long orderNumber,
        Long userId,
        BigDecimal originalTotalAmount,
        BigDecimal discountAmount,
        BigDecimal finalTotalAmount
) {
}
