package com.loopers.domain.outbox;

import java.util.List;

public interface OutboxRepository {
    void saveOutbox(Outbox outbox);

    List<Outbox> findPendingOutboxes(int limit);

    List<Outbox> findFailedAndRetryableOutboxes(int limit, int maxRetries);

    void markAsPublished(Long id);

    void markAsFailed(Long id, String message);
}

