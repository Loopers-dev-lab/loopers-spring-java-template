package com.loopers.domain.like.event;

/**
 * 좋아요 변경 이벤트
 * <p>
 * 좋아요 등록/취소 시 발행되는 이벤트입니다.
 * 집계 업데이트를 위해 사용됩니다.
 *
 * @param productId  상품 ID
 * @param userId     사용자 ID
 * @param action     좋아요 액션 (LIKE, UNLIKE)
 * @param countDelta 카운트 변화량 (+1 또는 -1)
 * @author hyunjikoh
 * @since 2025. 12. 10.
 */
public record LikeChangedEvent(
        Long productId,
        Long userId,
        LikeAction action,
        int countDelta
) {
    public enum LikeAction {
        LIKE,    // 좋아요 등록
        UNLIKE   // 좋아요 취소
    }
}
