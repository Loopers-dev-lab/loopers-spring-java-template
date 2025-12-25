package com.loopers.infrastructure.idempotent;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EventHandledRepository extends JpaRepository<EventHandled, Long> {

    boolean existsByEventId(String eventId);
}
