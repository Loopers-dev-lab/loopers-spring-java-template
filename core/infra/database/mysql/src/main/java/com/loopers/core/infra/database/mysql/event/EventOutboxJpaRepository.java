package com.loopers.core.infra.database.mysql.event;

import com.loopers.core.infra.database.mysql.event.entity.EventOutboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventOutboxJpaRepository extends JpaRepository<EventOutboxEntity, Long> {

    List<EventOutboxEntity> findAllByEventTypeAndAggregateTypeAndStatus(
            String eventType, String aggregateType, String status
    );
}
