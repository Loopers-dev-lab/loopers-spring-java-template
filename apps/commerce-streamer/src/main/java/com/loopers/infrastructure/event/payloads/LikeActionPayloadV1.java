package com.loopers.infrastructure.event.payloads;

/**
 * 좋아요 액션 이벤트 페이로드 V1
 *
 * @author hyunjikoh
 * @since 2025. 12. 18.
 */
public record LikeActionPayloadV1(
        Long productId,
        Long userId,
        String action  // "LIKE" or "UNLIKE"
) {
}