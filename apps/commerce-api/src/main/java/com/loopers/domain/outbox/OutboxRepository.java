package com.loopers.domain.outbox;

import java.util.List;

public interface OutboxRepository {
    OutboxEvent save(OutboxEvent outBoxEvent);

    List<OutboxEvent> findPending(int limit);
}
