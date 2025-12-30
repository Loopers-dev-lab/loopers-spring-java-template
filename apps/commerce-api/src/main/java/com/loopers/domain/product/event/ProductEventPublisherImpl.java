package com.loopers.domain.product.event;

import com.loopers.shared.event.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ProductEventPublisherImpl implements ProductEventPublisher {

    private final EventPublisher eventPublisher;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishProductCreated(ProductEvents.Created event) {
        eventPublisher.publish(event);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishProductUpdated(ProductEvents.Updated event) {
        eventPublisher.publish(event);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishProductDeleted(ProductEvents.Deleted event) {
        eventPublisher.publish(event);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishProductLikeCountChanged(ProductEvents.LikeCount event) {
        eventPublisher.publish(event);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishProductViewed(ProductEvents.Viewed event) {
        eventPublisher.publish(event);
    }
}

