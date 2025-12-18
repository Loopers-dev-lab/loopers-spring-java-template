package com.loopers.infrastructure.eventhandled;

import com.loopers.domain.eventhandled.EventHandled;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventHandledJpaRepository extends JpaRepository<EventHandled, String> {

  boolean existsByEventId(String eventId);
}
