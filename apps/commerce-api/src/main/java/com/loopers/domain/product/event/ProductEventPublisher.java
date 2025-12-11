package com.loopers.domain.product.event;

public interface ProductEventPublisher {
    void publishProductCreated(ProductEvents.Created event);
    void publishProductUpdated(ProductEvents.Updated event);
    void publishProductDeleted(ProductEvents.Deleted event);
    void publishProductLikeCountChanged(ProductEvents.LikeCount event);
}

