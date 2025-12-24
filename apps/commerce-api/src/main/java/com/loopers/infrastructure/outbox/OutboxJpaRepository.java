package com.loopers.infrastructure.outbox;

import com.loopers.domain.outbox.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;


public interface OutboxJpaRepository extends JpaRepository<OutboxEvent, Long> {

}
