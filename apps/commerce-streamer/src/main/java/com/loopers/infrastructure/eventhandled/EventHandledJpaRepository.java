package com.loopers.infrastructure.eventhandled;

import com.loopers.domain.eventhandled.EventHandled;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventHandledJpaRepository extends JpaRepository<EventHandled, String> {

  boolean existsByEventId(String eventId);

  @Query("SELECT e.eventId FROM EventHandled e WHERE e.eventId IN :eventIds")
  List<String> findEventIdsByEventIdIn(@Param("eventIds") List<String> eventIds);
}
