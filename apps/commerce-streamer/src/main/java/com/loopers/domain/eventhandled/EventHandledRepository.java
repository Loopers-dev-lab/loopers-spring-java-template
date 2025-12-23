package com.loopers.domain.eventhandled;

public interface EventHandledRepository {
    void saveEventHandled(EventHandled eventHandled);

    boolean existsByEventId(String eventId);
}

