package com.loopers.infrastructure.event;

import com.loopers.domain.event.EventHandled;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EventHandledJpaRepository extends JpaRepository<EventHandled, UUID> {
}
