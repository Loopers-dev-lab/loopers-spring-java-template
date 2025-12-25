package com.loopers.core.infra.database.mysql.event;

import com.loopers.core.infra.database.mysql.event.entity.EventInboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface EventInboxJpaRepository extends JpaRepository<EventInboxEntity, Long> {

    @Query("SELECT e FROM EventInboxEntity e WHERE e.eventId = :eventId")
    Optional<EventInboxEntity> findByEventId(String eventId);
}
