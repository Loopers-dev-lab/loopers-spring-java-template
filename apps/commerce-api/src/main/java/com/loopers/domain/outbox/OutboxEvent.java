package com.loopers.domain.outbox;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.ZonedDateTime;

@Entity
@Table(name = "outbox_events")
@Getter
public class OutboxEvent extends BaseEntity {
    @Column(nullable = false)
    private String aggregateType;  // "ORDER", "PAYMENT" 등

    @Column(nullable = false)
    private String aggregateId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;  // PENDING, PUBLISHED, FAILED

    // createdAt은 BaseEntity에서 자동으로 관리됨 (@PrePersist)

    private ZonedDateTime publishedAt;

    public static OutboxEvent create(
            String aggregateType,
            String aggregateId,
            String eventType,
            String payload
    ) {
        OutboxEvent event = new OutboxEvent();
        event.aggregateType = aggregateType;
        event.aggregateId = aggregateId;
        event.eventType = eventType;
        event.payload = payload;
        event.status = OutboxStatus.PENDING;
        return event;
    }

    public void markAsPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = ZonedDateTime.now();
    }

    public void markAsFailed() {
        this.status = OutboxStatus.FAILED;
    }
}
