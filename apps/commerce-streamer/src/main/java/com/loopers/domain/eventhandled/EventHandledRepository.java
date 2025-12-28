package com.loopers.domain.eventhandled;

import java.util.List;
import java.util.Set;

public interface EventHandledRepository {

  boolean existsByEventId(String eventId);

  void save(EventHandled eventHandled);

  Set<String> findExistingEventIds(List<String> eventIds);

  void saveAll(List<EventHandled> events);
}
