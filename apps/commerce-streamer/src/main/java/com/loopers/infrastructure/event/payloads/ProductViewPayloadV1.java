package com.loopers.infrastructure.event.payloads;

/**
 * 상품 조회 이벤트 페이로드 V1
 *
 * @author hyunjikoh
 * @since 2025. 12. 18.
 */
public record ProductViewPayloadV1(
        Long productId,
        Long userId
) {
}