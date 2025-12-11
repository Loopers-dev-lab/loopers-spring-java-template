package com.loopers.infrastructure.outbox;

import com.loopers.domain.outbox.EventOutbox;
import com.loopers.domain.outbox.OutboxStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EventOutboxJpaRepository extends JpaRepository<EventOutbox, Long> {

    @Query("SELECT e FROM EventOutbox e WHERE e.status = 'PENDING' ORDER BY e.createdAt ASC")
    List<EventOutbox> findByStatusOrderByCreatedAtAsc(OutboxStatus status);

    @Query("SELECT e FROM EventOutbox e WHERE e.status = 'FAILED' AND e.retryCount < 3 ORDER BY e.createdAt ASC")
    List<EventOutbox> findFailedEventsCanRetry();
}
