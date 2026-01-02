package com.loopers.domain.eventhandled;

import java.util.List;

public interface EventHandledRepository {
    boolean existsByEventId(String eventId);
    EventHandled save(EventHandled eventHandled);
    void saveAll(List<EventHandled> eventHandledList);

    List<EventHandled> findByEventId(String eventId);

    /**
     * 여러 eventId에 해당하는 EventHandled 조회 (N+1 방지)
     */
    List<EventHandled> findAllByEventIdIn(List<String> eventIds);
}
