package com.loopers.core.infra.database.mysql.event.entity;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.event.EventInbox;
import com.loopers.core.domain.event.type.AggregateType;
import com.loopers.core.domain.event.type.EventInboxStatus;
import com.loopers.core.domain.event.type.EventType;
import com.loopers.core.domain.event.vo.AggregateId;
import com.loopers.core.domain.event.vo.EventId;
import com.loopers.core.domain.event.vo.EventInboxId;
import com.loopers.core.domain.event.vo.EventPayload;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Optional;

@Entity
@Table(
        name = "event_inbox",
        indexes = {
                @Index(name = "idx_event_inbox_event_id", columnList = "event_id"),
                @Index(name = "idx_event_inbox_status", columnList = "status")
        }
)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventInboxEntity {

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

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static EventInboxEntity from(EventInbox eventInbox) {
        return new EventInboxEntity(
                Optional.ofNullable(eventInbox.getId().value())
                        .map(Long::parseLong)
                        .orElse(null),
                eventInbox.getEventId().value(),
                eventInbox.getAggregateType().name(),
                eventInbox.getAggregateId().value(),
                eventInbox.getEventType().name(),
                eventInbox.getStatus().name(),
                eventInbox.getPayload().value(),
                eventInbox.getCreatedAt().value(),
                eventInbox.getUpdatedAt().value()
        );
    }

    public EventInbox to() {
        return EventInbox.mappedBy(
                new EventInboxId(this.id.toString()),
                new EventId(this.eventId),
                AggregateType.valueOf(this.aggregateType),
                new AggregateId(this.aggregateId),
                EventType.valueOf(this.eventType),
                EventInboxStatus.valueOf(this.status),
                new EventPayload(this.payload),
                new CreatedAt(this.createdAt),
                new UpdatedAt(this.updatedAt)
        );
    }
}
