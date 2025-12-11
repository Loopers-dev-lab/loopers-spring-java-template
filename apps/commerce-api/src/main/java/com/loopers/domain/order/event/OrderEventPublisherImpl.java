package com.loopers.domain.order.event;

import com.loopers.shared.event.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * EventPublisher 구현체만 교체하면 됨
 */
@Component
@RequiredArgsConstructor
public class OrderEventPublisherImpl implements OrderEventPublisher {

    private final EventPublisher eventPublisher;
    
    private static final String TOPIC_ORDER_CREATED = "order.created.v1";

    @Override
    public void publishOrderCreated(OrderEvents.Created event) {
        String key = String.valueOf(event.orderId()); // 파티션 키로 사용
        eventPublisher.publish(TOPIC_ORDER_CREATED, key, event);
    }
}
