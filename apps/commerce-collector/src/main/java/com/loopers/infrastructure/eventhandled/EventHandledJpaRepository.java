package com.loopers.infrastructure.eventhandled;

import com.loopers.domain.eventhandled.EventHandled;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventHandledJpaRepository extends JpaRepository<EventHandled, Long> {
    boolean existsByEventId(String eventId);

    List<EventHandled> findByEventId(String eventId);
}
