package com.loopers.domain.event;

import java.util.Optional;

public interface EventHandledRepository {

    boolean existsByEventId(String eventId);

    EventHandled save(EventHandled eventHandled);
}
