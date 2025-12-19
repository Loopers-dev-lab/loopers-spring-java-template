package com.loopers.domain.event;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "outbox_events", indexes = {
        @Index(name = "idx_outbox_created_at_status", columnList = "createdAt, status")
})
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String aggregateType; // e.g., "ORDER", "PRODUCT"

    @Column(nullable = false)
    private String aggregateId;   // e.g., OrderId, ProductId

    @Column(nullable = false)
    private String type;          // e.g., "OrderCreated", "ProductUpdated"

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;       // JSON String of the event

    @Column(nullable = false)
    private String topic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime publishedAt;

    @Builder
    public OutboxEvent(String aggregateType, String aggregateId, String type, String payload, String topic) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.type = type;
        this.payload = payload;
        this.topic = topic;
        this.status = OutboxStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public void markAsPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    public void markAsFailed() {
        this.status = OutboxStatus.FAILED;
    }

    public enum OutboxStatus {
        PENDING, PUBLISHED, FAILED
    }
}

