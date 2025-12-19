package com.loopers.domain.product.event;

import com.loopers.shared.event.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductEventPublisherImpl implements ProductEventPublisher {

    private final EventPublisher eventPublisher;

    @Override
    public void publishProductCreated(ProductEvents.Created event) {
        eventPublisher.publish(event);
    }

    @Override
    public void publishProductUpdated(ProductEvents.Updated event) {
        eventPublisher.publish(event);
    }

    @Override
    public void publishProductDeleted(ProductEvents.Deleted event) {
        eventPublisher.publish(event);
    }

    @Override
    public void publishProductLikeCountChanged(ProductEvents.LikeCount event) {
        eventPublisher.publish(event);
    }

    @Override
    public void publishProductViewed(ProductEvents.Viewed event) {
        eventPublisher.publish(event);
    }
}

