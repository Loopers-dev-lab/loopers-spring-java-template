package com.loopers.application.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.outbox.EventOutbox;
import com.loopers.domain.outbox.EventOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutboxEventService {

    private final ObjectMapper objectMapper;
    private final EventOutboxRepository outboxRepository;

    @Transactional
    public EventOutbox saveEvent(String aggregateType,
                                 String aggregateId,
                                 String eventType,
                                 String eventKey,
                                 Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            EventOutbox outbox = EventOutbox.of(aggregateType, aggregateId, eventType, eventKey, json);
            return outboxRepository.save(outbox);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Outbox payload 직렬화 실패", e);
        }
    }
}




