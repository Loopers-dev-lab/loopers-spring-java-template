package com.loopers.infrastructure.outbox;

import com.loopers.domain.outbox.EventOutbox;
import com.loopers.domain.outbox.EventOutboxRepository;
import com.loopers.domain.outbox.OutboxStatus;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EventOutboxRepositoryImpl implements EventOutboxRepository {

    private final EventOutboxJpaRepository jpaRepository;

    @Override
    public EventOutbox save(EventOutbox outbox) {
        return jpaRepository.save(outbox);
    }

    @Override
    public List<EventOutbox> findPendingEvents() {
        return jpaRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
    }

    @Override
    public List<EventOutbox> findFailedEventsCanRetry() {
        return jpaRepository.findFailedEventsCanRetry();
    }
}
