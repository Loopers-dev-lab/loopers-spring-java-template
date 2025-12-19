package com.loopers.domain.outbox;

import java.util.List;

public interface OutboxEventRepository {
    OutboxEvent save(OutboxEvent event);
    List<OutboxEvent> findPendingEvents(int limit);
    List<OutboxEvent> findFailedEventsForRetry(int maxRetryCount, int limit);
}
