package com.loopers.application.like.event;

import java.time.LocalDateTime;

/**
 * 좋아요 토글 이벤트
 * - 좋아요가 추가되거나 제거된 후 발행되는 이벤트
 * - 상품의 좋아요 수 집계를 비동기로 처리한다 (eventual consistency)
 */
public record LikeToggledEvent(
        String userId,
        Long productId,
        boolean isAdded,
        LocalDateTime occurredAt
) {
    public static LikeToggledEvent added(String userId, Long productId) {
        return new LikeToggledEvent(userId, productId, true, LocalDateTime.now());
    }

    public static LikeToggledEvent removed(String userId, Long productId) {
        return new LikeToggledEvent(userId, productId, false, LocalDateTime.now());
    }
}
