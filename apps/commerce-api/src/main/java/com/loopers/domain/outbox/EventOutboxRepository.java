package com.loopers.domain.outbox;

import java.util.List;

public interface EventOutboxRepository {

    EventOutbox save(EventOutbox outbox);

    List<EventOutbox> findPendingEvents();

    List<EventOutbox> findFailedEventsCanRetry();
}
