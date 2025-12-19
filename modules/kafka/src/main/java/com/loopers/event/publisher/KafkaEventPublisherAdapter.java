package com.loopers.event.publisher;

import com.loopers.event.outbox.TransactionalOutboxEventPublisher;
import com.loopers.shared.event.DomainEvent;
import com.loopers.shared.event.EventPublisher;
import com.loopers.shared.event.EventTopicRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Kafka 기반 이벤트 발행 구현체
 * Transactional Outbox 패턴을 사용하여 트랜잭션 일관성을 보장합니다.
 * 
 * 기본 발행 전략으로 사용됩니다 (@Primary).
 * 
 * EventTopicRegistry는 각 애플리케이션에서 제공해야 하며, 없으면 기본 토픽명을 사용합니다.
 */
@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class KafkaEventPublisherAdapter implements EventPublisher {
    
    private final TransactionalOutboxEventPublisher outboxEventPublisher;
    
    @Autowired(required = false)
    private EventTopicRegistry topicRegistry;
    
    @Override
    public void publish(DomainEvent event) {
        String topic = getTopic(event);
        String key = event.getPartitionKey();
        
        log.debug("Publishing event to Kafka via Outbox - topic: {}, key: {}, type: {}", 
                topic, key, event.getClass().getSimpleName());
        
        outboxEventPublisher.publish(topic, key, event);
    }
    
    /**
     * 이벤트 타입에 해당하는 토픽을 결정합니다.
     * EventTopicRegistry가 있으면 사용하고, 없으면 기본 토픽명을 생성합니다.
     */
    private String getTopic(DomainEvent event) {
        if (topicRegistry != null) {
            try {
                return topicRegistry.getTopic(event.getClass());
            } catch (IllegalArgumentException e) {
                log.warn("Topic not found in registry for {}, using default topic name", event.getClass().getSimpleName());
            }
        }
        // 기본 토픽명 생성 (클래스명 기반)
        return generateDefaultTopicName(event.getClass().getSimpleName());
    }
    
    /**
     * 기본 토픽명을 생성합니다.
     * 예: OrderEvents.Created -> order.created.v1
     */
    private String generateDefaultTopicName(String className) {
        // 간단한 변환 로직 (실제로는 더 정교한 변환이 필요할 수 있음)
        String simpleName = className.replace("Events$", "").toLowerCase();
        return simpleName + ".v1";
    }
}
