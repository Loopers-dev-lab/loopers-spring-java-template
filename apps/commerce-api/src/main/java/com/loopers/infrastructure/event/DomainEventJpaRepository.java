package com.loopers.infrastructure.event;

import com.loopers.domain.common.event.DomainEvent;
import com.loopers.domain.common.event.EventStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DomainEventJpaRepository extends JpaRepository<DomainEvent, UUID> {
    List<DomainEvent> findByStatus(EventStatus status, Pageable pageable);
}
