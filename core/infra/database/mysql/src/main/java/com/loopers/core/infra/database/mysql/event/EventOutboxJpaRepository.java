package com.loopers.core.infra.database.mysql.event;

import com.loopers.core.infra.database.mysql.event.entity.EventOutboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventOutboxJpaRepository extends JpaRepository<EventOutboxEntity, Long> {
}
