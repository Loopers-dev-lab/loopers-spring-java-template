package com.loopers.domain.like.event;

public interface LikeEventPublisher {

    void publish(LikeEvent.ProductLiked event);

    void publish(LikeEvent.ProductUnliked event);
}

