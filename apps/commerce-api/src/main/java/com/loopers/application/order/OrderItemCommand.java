package com.loopers.application.order;

import lombok.Builder;

/**
 * 주문 항목 커맨드
 *
 * @author hyunjikoh
 * @since 2025. 11. 14.
 */
@Builder
public record OrderItemCommand(
        Long productId,
        Integer quantity,
        Long couponId
) {
}
