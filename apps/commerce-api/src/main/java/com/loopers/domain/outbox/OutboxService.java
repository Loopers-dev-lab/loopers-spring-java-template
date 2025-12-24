package com.loopers.domain.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void saveEvent(
            String aggregateType,
            String aggregateId,
            String eventType,
            String topic,
            String partitionKey,
            Object payload
    ) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            OutboxEvent event = OutboxEvent.create(
                    aggregateType,
                    aggregateId,
                    eventType,
                    topic,
                    partitionKey,
                    payloadJson
            );
            outboxEventRepository.save(event);
            log.debug("Outbox 이벤트 저장: aggregateType={}, aggregateId={}, eventType={}",
                    aggregateType, aggregateId, eventType);
        } catch (JsonProcessingException e) {
            log.error("Outbox 이벤트 직렬화 실패: aggregateType={}, aggregateId={}",
                    aggregateType, aggregateId, e);
            throw new RuntimeException("이벤트 직렬화 실패", e);
        }
    }
}
