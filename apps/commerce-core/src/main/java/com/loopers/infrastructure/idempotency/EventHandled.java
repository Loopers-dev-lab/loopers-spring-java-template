package com.loopers.infrastructure.idempotency;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_event_handled", indexes = {
    @Index(name = "idx_event_handled_event_id", columnList = "event_id", unique = true)
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventHandled extends BaseEntity {

    @Column(name = "event_id", nullable = false, length = 36, unique = true)
    private String eventId;  // UUID 문자열

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Column(name = "handled_at", nullable = false)
    private LocalDateTime handledAt;

    @Column(name = "handler_name", length = 100)
    private String handlerName;

    @Builder
    public EventHandled(
            String eventId,
            String eventType,
            String aggregateId,
            String handlerName
    ) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.handlerName = handlerName;
        this.handledAt = LocalDateTime.now();
    }
}

