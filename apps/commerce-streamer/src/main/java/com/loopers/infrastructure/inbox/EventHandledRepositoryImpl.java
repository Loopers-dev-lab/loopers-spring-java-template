package com.loopers.infrastructure.inbox;

import com.loopers.domain.event.EventHandled;
import com.loopers.domain.event.EventHandledRepository;
import com.loopers.domain.event.EventStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class EventHandledRepositoryImpl implements EventHandledRepository {
  private final EventHandledJpaRepository outboxJpaRepository;

  public boolean existsByBusinessKey(String businessKey) {
    return outboxJpaRepository.existsByBusinessKey(businessKey);
  }

  @Override
  public EventHandled save(EventHandled eventHandled) {
    return outboxJpaRepository.save(eventHandled);
  }

  @Override
  public List<EventHandled> findByStatus(EventStatus status) {
    return outboxJpaRepository.findByStatus(status);
  }

  @Override
  public Optional<EventHandled> findById(Long id) {
    return outboxJpaRepository.findById(id);
  }

  List<EventHandled> findByHandledAtBefore(LocalDateTime before) {
    return outboxJpaRepository.findByHandledAtBefore(before);
  }
}
