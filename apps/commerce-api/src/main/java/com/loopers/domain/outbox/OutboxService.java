package com.loopers.domain.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void saveEvent(String aggregateType, String aggregateId, String eventType, Object eventData) {
        try {
            String payload = objectMapper.writeValueAsString(eventData);
            OutboxEvent outboxEvent = new OutboxEvent(aggregateType, aggregateId, eventType, payload);
            outboxEventRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event data", e);
        }
    }

    @Transactional
    public void markAsProcessed(Long outboxEventId) {
        OutboxEvent outboxEvent = outboxEventRepository.findById(outboxEventId)
            .orElseThrow(() -> new RuntimeException("OutboxEvent not found: " + outboxEventId));
        outboxEvent.markAsProcessed();
    }

    @Transactional
    public void markAsFailed(Long outboxEventId, String errorMessage) {
        OutboxEvent outboxEvent = outboxEventRepository.findById(outboxEventId)
            .orElseThrow(() -> new RuntimeException("OutboxEvent not found: " + outboxEventId));
        outboxEvent.markAsFailed(errorMessage);
    }
}