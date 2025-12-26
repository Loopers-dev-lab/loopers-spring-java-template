package com.loopers.infrastructure.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventHandledRepository extends JpaRepository<EventHandled, String> {
    Optional<EventHandled> findByEventId(String eventId);
    boolean existsByEventId(String eventId);
}

