package com.loopers.domain.event;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query("SELECT o FROM OutboxEvent o WHERE o.status = 'PENDING' AND o.createdAt <= :beforeTime ORDER BY o.createdAt ASC")
    List<OutboxEvent> findAllPendingEventsBefore(@Param("beforeTime") LocalDateTime beforeTime, Pageable pageable);
}

