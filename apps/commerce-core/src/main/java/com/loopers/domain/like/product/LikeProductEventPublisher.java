package com.loopers.domain.like.product;

public interface LikeProductEventPublisher {
    void publishLikeEvent(LikeProductEvent event);
}
