package com.loopers.domain.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;

    public OutboxEvent createOutboxEvent(
            String aggregateType,
            String aggregateId,
            String eventType,
            String payload
    ) {
        OutboxEvent outboxEvent = OutboxEvent.create(
                aggregateType,
                aggregateId,
                eventType,
                payload
        );

        OutboxEvent savedOutboxEvent = outboxEventRepository.save(outboxEvent);

        return savedOutboxEvent;
    }

    public List<OutboxEvent> getPendingEvents(int limit) {
        return outboxEventRepository
                .findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING, limit);
    }

    public void save(OutboxEvent outboxEvent) {
        outboxEventRepository.save(outboxEvent);
    }
}
