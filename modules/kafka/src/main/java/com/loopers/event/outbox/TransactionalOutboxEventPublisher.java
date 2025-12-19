package com.loopers.event.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.event.OutboxEvent;
import com.loopers.domain.event.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionalOutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public <T> void publish(String topic, String key, T event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            String type = event.getClass().getSimpleName();
            
            // aggregateType은 단순화를 위해 토픽명 사용, 실제로는 이벤트 인터페이스 등에서 추출 가능
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType(topic) 
                    .aggregateId(key)
                    .type(type)
                    .payload(payload)
                    .topic(topic)
                    .build();

            outboxEventRepository.save(outboxEvent);
            log.info("Saved event to Outbox - topic: {}, key: {}, type: {}", topic, key, type);
            
        } catch (Exception e) {
            log.error("Failed to save event to Outbox", e);
            throw new RuntimeException("Failed to save event to Outbox", e);
        }
    }
}

