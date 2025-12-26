package com.loopers.domain.event.outbox;

import java.util.List;

public interface OutboxEventRepository {
    List<OutboxEvent> findPendingEventsForUpdate(OutboxStatus status);

    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxStatus status);

    OutboxEvent save(OutboxEvent outboxEvent);
}

