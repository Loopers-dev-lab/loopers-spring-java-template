package com.loopers.infrastructure.event.outbox;

import com.loopers.domain.event.outbox.OutboxEvent;
import com.loopers.domain.event.outbox.OutboxStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEvent, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OutboxEvent o " +
            "WHERE o.status = :status " +
            "ORDER BY o.createdAt ASC")
    List<OutboxEvent> findPendingEventsForUpdate(@Param("status") OutboxStatus status);

    @Query("SELECT o FROM OutboxEvent o " +
            "WHERE o.status = :status " +
            "ORDER BY o.createdAt ASC")
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(@Param("status") OutboxStatus status);
}

