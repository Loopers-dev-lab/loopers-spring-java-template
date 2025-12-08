package com.loopers.domain.like.event;

import com.loopers.domain.like.Like;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 좋아요 생성 이벤트
 * - 좋아요가 생성되었을 때 발행
 * - 후속 처리: 상품 좋아요 수 증가
 */
public record LikeCreatedEvent(
    String eventId,
    Long likeId,
    String userId,
    Long productId,
    LocalDateTime createdAt
) {
    public static LikeCreatedEvent from(Like like) {
        return new LikeCreatedEvent(
            UUID.randomUUID().toString(),
            like.getId(),
            like.getUserId(),
            like.getProductId(),
            LocalDateTime.now()
        );
    }
}
