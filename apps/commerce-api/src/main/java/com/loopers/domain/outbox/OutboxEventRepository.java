package com.loopers.domain.outbox;

import java.util.List;
import java.util.Optional;

public interface OutboxEventRepository {

    OutboxEvent save(OutboxEvent event);

    Optional<OutboxEvent> findByEventId(String eventId);

    List<OutboxEvent> findTopPending(int size);
}

