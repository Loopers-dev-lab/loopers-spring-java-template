package com.loopers.infrastructure.eventhandled;

import com.loopers.domain.eventhandled.EventHandled;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventHandledJpaRepository extends JpaRepository<EventHandled, Long> {
    boolean existsByEventId(String eventId);

    List<EventHandled> findByEventId(String eventId);

    /**
     * 여러 eventId에 해당하는 EventHandled 조회 (N+1 방지)
     * SELECT * FROM event_handled WHERE event_id IN (...)
     */
    List<EventHandled> findAllByEventIdIn(List<String> eventIds);
}
