package com.loopers.infrastructure.inbox;

import com.loopers.domain.event.EventHandled;
import com.loopers.domain.event.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface EventHandledJpaRepository extends JpaRepository<EventHandled, Long> {
  @Query("SELECT e FROM EventHandled e WHERE e.handledAt < :before")
  List<EventHandled> findByHandledAtBefore(LocalDateTime before);

  boolean existsByBusinessKey(String businessKey);

  List<EventHandled> findByStatus(EventStatus status);
}
