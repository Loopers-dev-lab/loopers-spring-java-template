package com.loopers.domain.eventhandled;

public interface EventHandledRepository {

    boolean existsByEventId(String eventId);

    EventHandled save(EventHandled eventHandled);
}
