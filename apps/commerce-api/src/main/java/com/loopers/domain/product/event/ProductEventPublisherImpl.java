package com.loopers.domain.product.event;

import com.loopers.shared.event.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * EventPublisher 구현체만 교체하면 됨
 */
@Component
@RequiredArgsConstructor
public class ProductEventPublisherImpl implements ProductEventPublisher {

    private final EventPublisher eventPublisher;
    
    private static final String TOPIC_PRODUCT_CREATED = "product.created.v1";
    private static final String TOPIC_PRODUCT_UPDATED = "product.updated.v1";
    private static final String TOPIC_PRODUCT_DELETED = "product.deleted.v1";
    private static final String TOPIC_PRODUCT_LIKE_COUNT_CHANGED = "product.like-count-changed.v1";

    @Override
    public void publishProductCreated(ProductEvents.Created event) {
        String key = String.valueOf(event.productId()); // 파티션 키로 사용
        eventPublisher.publish(TOPIC_PRODUCT_CREATED, key, event);
    }

    @Override
    public void publishProductUpdated(ProductEvents.Updated event) {
        String key = String.valueOf(event.productId()); // 파티션 키로 사용
        eventPublisher.publish(TOPIC_PRODUCT_UPDATED, key, event);
    }

    @Override
    public void publishProductDeleted(ProductEvents.Deleted event) {
        String key = String.valueOf(event.productId()); // 파티션 키로 사용
        eventPublisher.publish(TOPIC_PRODUCT_DELETED, key, event);
    }

    @Override
    public void publishProductLikeCountChanged(ProductEvents.LikeCount event) {
        String key = String.valueOf(event.productId()); // 파티션 키로 사용
        eventPublisher.publish(TOPIC_PRODUCT_LIKE_COUNT_CHANGED, key, event);
    }
}

