package com.loopers.domain.like.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 좋아요 삭제 이벤트
 * - 좋아요가 취소되었을 때 발행
 * - 후속 처리: 상품 좋아요 수 감소
 */
public record LikeDeletedEvent(
    String eventId,
    String userId,
    Long productId,
    LocalDateTime deletedAt
) {
    public static LikeDeletedEvent of(String userId, Long productId) {
        return new LikeDeletedEvent(
            UUID.randomUUID().toString(),
            userId,
            productId,
            LocalDateTime.now()
        );
    }
}
