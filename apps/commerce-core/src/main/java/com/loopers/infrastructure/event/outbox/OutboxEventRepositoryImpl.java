package com.loopers.infrastructure.event.outbox;

import com.loopers.domain.event.outbox.OutboxEvent;
import com.loopers.domain.event.outbox.OutboxEventRepository;
import com.loopers.domain.event.outbox.OutboxStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class OutboxEventRepositoryImpl implements OutboxEventRepository {
    private final OutboxEventJpaRepository jpaRepository;

    @Override
    public List<OutboxEvent> findPendingEventsForUpdate(OutboxStatus status) {
        return jpaRepository.findPendingEventsForUpdate(status);
    }

    @Override
    public List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxStatus status) {
        return jpaRepository.findByStatusOrderByCreatedAtAsc(status);
    }

    @Override
    public OutboxEvent save(OutboxEvent outboxEvent) {
        return jpaRepository.save(outboxEvent);
    }
}

