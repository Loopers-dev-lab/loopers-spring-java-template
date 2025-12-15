package com.loopers.core.infra.database.mysql.event.entity;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.event.EventOutbox;
import com.loopers.core.domain.event.type.AggregateType;
import com.loopers.core.domain.event.type.EventOutboxStatus;
import com.loopers.core.domain.event.type.EventType;
import com.loopers.core.domain.event.vo.AggregateId;
import com.loopers.core.domain.event.vo.EventId;
import com.loopers.core.domain.event.vo.EventOutboxId;
import com.loopers.core.domain.event.vo.EventPayload;
import com.loopers.core.domain.event.vo.PublishedAt;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Optional;

@Entity
@Table(
        name = "event_outbox",
        indexes = {
                @Index(name = "idx_event_outbox_event_id", columnList = "event_id"),
                @Index(name = "idx_event_outbox_aggregate_type_event_type_status", columnList = "aggregate_type,event_type,status")
        }
)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventOutboxEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String eventId;

    @Column(nullable = false)
    private String aggregateType;

    @Column(nullable = false)
    private String aggregateId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String status;

    @Lob
    @Column(nullable = false)
    private String payload;

    private LocalDateTime publishedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static EventOutboxEntity from(EventOutbox eventOutbox) {
        return new EventOutboxEntity(
                Optional.ofNullable(eventOutbox.getId().value())
                        .map(Long::parseLong)
                        .orElse(null),
                eventOutbox.getEventId().value(),
                eventOutbox.getAggregateType().name(),
                eventOutbox.getAggregateId().value(),
                eventOutbox.getEventType().name(),
                eventOutbox.getStatus().name(),
                eventOutbox.getPayload().value(),
                eventOutbox.getPublishedAt().value(),
                eventOutbox.getCreatedAt().value(),
                eventOutbox.getUpdatedAt().value()
        );
    }

    public EventOutbox to() {
        return EventOutbox.mappedBy(
                new EventOutboxId(this.id.toString()),
                new EventId(this.eventId),
                AggregateType.valueOf(this.aggregateType),
                new AggregateId(this.aggregateId),
                EventType.valueOf(this.eventType),
                EventOutboxStatus.valueOf(this.status),
                new EventPayload(this.payload),
                new PublishedAt(this.publishedAt),
                new CreatedAt(this.createdAt),
                new UpdatedAt(this.updatedAt)
        );
    }

}
