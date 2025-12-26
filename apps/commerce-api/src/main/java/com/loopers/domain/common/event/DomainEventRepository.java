package com.loopers.domain.common.event;

import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DomainEventRepository {

    List<DomainEvent> findByStatus(EventStatus eventStatus, Pageable pageable);

    DomainEvent save(DomainEvent event);

    Optional<DomainEvent> findById(UUID id);
}
