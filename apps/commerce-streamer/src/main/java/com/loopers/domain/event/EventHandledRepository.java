package com.loopers.domain.event;

import java.util.List;
import java.util.Optional;

public interface EventHandledRepository {

  boolean existsByBusinessKey(String businessKey);

  EventHandled save(EventHandled eventHandled);

  List<EventHandled> findByStatus(EventStatus status);

  Optional<EventHandled> findById(Long id);
}
