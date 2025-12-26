package com.loopers.domain.event;

import java.util.UUID;

public interface EventHandledRepository {
    boolean existsByEventId(UUID eventId);

    void save(EventHandled from);
}
