package com.loopers.domain.like.product;

import com.loopers.domain.event.BaseEvent;
import com.loopers.domain.event.EventType;
import lombok.Getter;

/**
 * 상품 좋아요 이벤트 클래스
 * 이 이벤트는 사용자가 상품에 좋아요를 누르거나 취소할 때 발생합니다.
 * 상품 ID, 사용자 ID, 브랜드 ID, 좋아요 상태를 포함합니다.
 * 중복된 좋아요 이벤트는 허용되지 않습니다.
 */
@Getter
public class LikeProductEvent extends BaseEvent {
    private final Long productId;
    private final Long userId;
    private final Long brandId;
    private final Boolean liked;

    private LikeProductEvent(EventType eventType, Long productId, Long userId, Long brandId, Boolean liked) {
        super(eventType);
        if (productId == null) {
            throw new IllegalArgumentException("상품 ID는 필수입니다.");
        }
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }
        if (brandId == null) {
            throw new IllegalArgumentException("브랜드 ID는 필수입니다.");
        }
        if (liked == null) {
            throw new IllegalArgumentException("좋아요 상태는 필수입니다.");
        }
        this.productId = productId;
        this.userId = userId;
        this.brandId = brandId;
        this.liked = liked;
    }

    public static LikeProductEvent create(EventType eventType, Long productId, Long userId, Long brandId, Boolean liked) {
        return new LikeProductEvent(eventType, productId, userId, brandId, liked);
    }
}
