package com.loopers.infrastructure.event.payloads;

import java.util.List;

import com.loopers.domain.order.OrderItemEntity;

/**
 *
 * @author hyunjikoh
 * @since 2025. 12. 17.
 */
public record PaymentSuccessPayloadV1(
        Long orderNumber,
        List<Item> items
) {
    public record Item(Long productId, Integer quantity) {
    }
}
