package com.loopers.domain.outbox;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.ZonedDateTime;

@Entity
@Table(name = "event_outbox")
@Getter
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 64)
    private String eventId;

    @Column(nullable = false, length = 100)
    private String topic;

    @Column(name = "partition_key", nullable = false, length = 100)
    private String partitionKey;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "occurred_at", nullable = false)
    private ZonedDateTime occurredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    protected OutboxEvent() {
    }

    private OutboxEvent(
            String eventId,
            String topic,
            String partitionKey,
            String eventType,
            String payload,
            ZonedDateTime occurredAt
    ) {
        this.eventId = eventId;
        this.topic = topic;
        this.partitionKey = partitionKey;
        this.eventType = eventType;
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
        this.attemptCount = 0;
        this.occurredAt = occurredAt;
    }

    public static OutboxEvent pending(
            String eventId,
            String topic,
            String partitionKey,
            String eventType,
            String payload,
            ZonedDateTime occurredAt
    ) {
        return new OutboxEvent(eventId, topic, partitionKey, eventType, payload, occurredAt);
    }

    public void markSent() {
        this.status = OutboxStatus.SENT;
        this.lastError = null;
    }

    public void markFailed(String message) {
        this.status = OutboxStatus.FAILED;
        this.lastError = message;
    }

    public void markPendingForRetry(String message) {
        this.status = OutboxStatus.PENDING;
        this.lastError = message;
    }

    public void increaseAttempt() {
        this.attemptCount += 1;
    }

    @PrePersist
    void onCreate() {
        ZonedDateTime now = ZonedDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = ZonedDateTime.now();
    }
}

