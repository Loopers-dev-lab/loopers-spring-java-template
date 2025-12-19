package com.loopers.domain.like.event;

import com.loopers.shared.event.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LikeEventPublisherImpl implements LikeEventPublisher {

    private final EventPublisher eventPublisher;

    @Override
    public void publishProductLikeSaved(LikeEvents.ProductLikeSaved event) {
        eventPublisher.publish(event);
    }

    @Override
    public void publishProductLikeDeleted(LikeEvents.ProductLikeDeleted event) {
        eventPublisher.publish(event);
    }

    @Override
    public void publishLikeCountChanged(LikeEvents.LikeCountChanged event) {
        eventPublisher.publish(event);
    }
}

