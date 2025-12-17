package com.loopers.domain.event.outbox;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.infrastructure.event.DomainEventEnvelope;
import com.loopers.infrastructure.event.DomainEventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRelay {

    private final OutboxRepository outboxRepository;
    private final DomainEventPublisher domainEventPublisher;

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void relay() {
        final List<OutboxEventEntity> readyEvents =
                outboxRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.READY);

        for (OutboxEventEntity e : readyEvents) {
            try {
                final DomainEventEnvelope envelope = new DomainEventEnvelope(
                        e.getEventId(),
                        e.getEventType(),
                        e.getVersion(),
                        e.getOccurredAtEpochMillis(),
                        e.getPayloadJson()
                );

                domainEventPublisher.publish(e.getTopic(), e.getMessageKey(), envelope);

                e.markSent();
            } catch (Exception ex) {
                e.markFailed();
                log.warn("Outbox relay 실패 - eventId={}, retryCount={}", e.getEventId(), e.getRetryCount(), ex);
            }
        }
    }
}
