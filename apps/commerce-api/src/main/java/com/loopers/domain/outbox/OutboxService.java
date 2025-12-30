package com.loopers.domain.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public String nextEventId() {
        return UUID.randomUUID().toString();
    }

    @Transactional
    public void append(String eventId, String topic, String partitionKey, String eventType, Object payload, ZonedDateTime occurredAt) {
        try {
            String serializedPayload = objectMapper.writeValueAsString(payload);
            OutboxEvent event = OutboxEvent.pending(
                    eventId,
                    topic,
                    partitionKey,
                    eventType,
                    serializedPayload,
                    occurredAt
            );
            outboxEventRepository.save(event);
        } catch (JsonProcessingException exception) {
            throw new CoreException(ErrorType.INTERNAL_ERROR, "이벤트 직렬화에 실패했습니다.");
        }
    }

    @Transactional(readOnly = true)
    public List<OutboxEvent> fetchPending(int size) {
        return outboxEventRepository.findTopPending(size);
    }

    @Transactional
    public void markSent(OutboxEvent event) {
        event.markSent();
        outboxEventRepository.save(event);
    }

    @Transactional
    public void markFailed(OutboxEvent event, String message) {
        event.increaseAttempt();
        event.markPendingForRetry(message);
        outboxEventRepository.save(event);
    }
}
