package com.loopers.infrastructure.event.payloads;

import java.util.List;

/**
 * 결제 성공 이벤트 페이로드 V1
 *
 * @author hyunjikoh
 * @since 2025. 12. 18.
 */
public record PaymentSuccessPayloadV1(
        Long orderNumber,
        List<Item> items
) {
    public record Item(Long productId, Integer quantity) {
    }
}