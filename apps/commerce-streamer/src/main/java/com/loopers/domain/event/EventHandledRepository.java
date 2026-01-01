package com.loopers.domain.event;

import java.util.List;
import java.util.Optional;

public interface EventHandledRepository {

  boolean existsByBusinessKey(String businessKey);

  EventHandled save(EventHandled eventHandled);

  List<EventHandled> saveAll(List<EventHandled> eventHandledList);

  List<EventHandled> findByStatus(EventStatus status);

  List<EventHandled> findByStatusAndEventType(EventStatus status, String eventType);

  Optional<EventHandled> findById(Long id);

}
