package com.loopers.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Outbox 이벤트 저장 서비스
 * - Application Event를 Outbox 테이블에 저장
 * - JSON 직렬화 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventWriter {

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * Application Event를 Outbox 테이블에 저장
     *
     * @param partitionKey Kafka partition key (productId, orderNo 등)
     * @param eventType 이벤트 타입 (PRODUCT_LIKED, ORDER_PLACED 등)
     * @param eventPayload Application Event 객체
     */
    public void write(String partitionKey, String eventType, Object eventPayload) {
        try {
            String payload = objectMapper.writeValueAsString(eventPayload);

            OutboxEvent outboxEvent = OutboxEvent.create(
                partitionKey,
                eventType,
                payload
            );

            outboxRepository.save(outboxEvent);

            log.debug("[Outbox] 이벤트 저장 완료 - type: {}, key: {}, id: {}",
                eventType, partitionKey, outboxEvent.getId());

        } catch (JsonProcessingException e) {
            log.error("[Outbox] 이벤트 직렬화 실패 - type: {}, key: {}",
                eventType, partitionKey, e);
            throw new IllegalStateException("Failed to serialize event", e);
        }
    }
}
