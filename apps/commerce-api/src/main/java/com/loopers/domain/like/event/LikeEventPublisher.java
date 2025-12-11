package com.loopers.domain.like.event;

public interface LikeEventPublisher {
    void publishProductLikeSaved(LikeEvents.ProductLikeSaved event);
    void publishProductLikeDeleted(LikeEvents.ProductLikeDeleted event);
    void publishLikeCountChanged(LikeEvents.LikeCountChanged event);
}

