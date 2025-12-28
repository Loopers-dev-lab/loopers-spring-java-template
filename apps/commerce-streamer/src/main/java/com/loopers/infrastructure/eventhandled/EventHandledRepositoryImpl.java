package com.loopers.infrastructure.eventhandled;

import com.loopers.domain.eventhandled.EventHandled;
import com.loopers.domain.eventhandled.EventHandledRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EventHandledRepositoryImpl implements EventHandledRepository {

  private final EventHandledJpaRepository jpaRepository;

  @Override
  public boolean existsByEventId(String eventId) {
    return jpaRepository.existsByEventId(eventId);
  }

  @Override
  public void save(EventHandled eventHandled) {
    jpaRepository.save(eventHandled);
  }

  @Override
  public Set<String> findExistingEventIds(List<String> eventIds) {
    if (eventIds == null || eventIds.isEmpty()) {
      return Set.of();
    }
    return new HashSet<>(jpaRepository.findEventIdsByEventIdIn(eventIds));
  }

  @Override
  public void saveAll(List<EventHandled> events) {
    if (events != null && !events.isEmpty()) {
      jpaRepository.saveAll(events);
    }
  }
}
