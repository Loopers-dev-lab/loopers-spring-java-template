package com.loopers.infrastructure.eventhandled;

import com.loopers.domain.eventhandled.EventHandled;
import com.loopers.domain.eventhandled.EventHandledRepository;
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
}
