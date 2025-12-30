package com.loopers.infrastructure.like;

import com.loopers.domain.like.event.LikeEvent;
import com.loopers.domain.like.event.LikeEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LikeCoreEventPublisher implements LikeEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(LikeEvent.ProductLiked event) {
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    public void publish(LikeEvent.ProductUnliked event) {
        applicationEventPublisher.publishEvent(event);
    }
}

