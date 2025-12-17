package com.loopers.domain.event.outbox;

import java.util.List;

/**
 *
 * @author hyunjikoh
 * @since 2025. 12. 17.
 */
public interface OutboxRepository {
    List<OutboxEventEntity> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus outboxStatus);

    OutboxEventEntity save(OutboxEventEntity ready);
}
