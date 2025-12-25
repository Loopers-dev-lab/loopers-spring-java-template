package com.loopers.domain.eventhandled;

import java.util.List;

public interface EventHandledRepository {
    boolean existsByEventId(String eventId);
    EventHandled save(EventHandled eventHandled);

    List<EventHandled> findByEventId(String eventId);
}
