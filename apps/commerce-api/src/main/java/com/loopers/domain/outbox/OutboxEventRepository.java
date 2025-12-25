package com.loopers.domain.outbox;

import java.util.List;

public interface OutboxEventRepository {
    OutboxEvent save(OutboxEvent outboxEvent);

    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxStatus outboxStatus, int limit);
}
