package com.loopers.domain.like.event;

import com.loopers.shared.event.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * EventPublisher 구현체만 교체하면 됨
 */
@Component
@RequiredArgsConstructor
public class LikeEventPublisherImpl implements LikeEventPublisher {

    private final EventPublisher eventPublisher;
    
    private static final String TOPIC_PRODUCT_LIKE_SAVED = "like.product-saved.v1";
    private static final String TOPIC_PRODUCT_LIKE_DELETED = "like.product-deleted.v1";
    private static final String TOPIC_LIKE_COUNT_CHANGED = "like.count-changed.v1";

    @Override
    public void publishProductLikeSaved(LikeEvents.ProductLikeSaved event) {
        String key = String.valueOf(event.productId()); // 파티션 키로 사용
        eventPublisher.publish(TOPIC_PRODUCT_LIKE_SAVED, key, event);
    }

    @Override
    public void publishProductLikeDeleted(LikeEvents.ProductLikeDeleted event) {
        String key = String.valueOf(event.productId()); // 파티션 키로 사용
        eventPublisher.publish(TOPIC_PRODUCT_LIKE_DELETED, key, event);
    }

    @Override
    public void publishLikeCountChanged(LikeEvents.LikeCountChanged event) {
        String key = String.valueOf(event.productId()); // 파티션 키로 사용
        eventPublisher.publish(TOPIC_LIKE_COUNT_CHANGED, key, event);
    }
}

