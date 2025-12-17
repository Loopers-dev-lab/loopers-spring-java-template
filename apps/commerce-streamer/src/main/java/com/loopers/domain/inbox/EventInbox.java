package com.loopers.domain.inbox;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Event Inbox 패턴 구현
 * - Consumer의 멱등성 보장을 위한 이벤트 수신 기록
 * - eventId를 Unique Key로 사용하여 중복 처리 방지
 */
@Getter
@Entity
@Table(
    name = "event_inbox",
    indexes = {
        @Index(name = "idx_event_id", columnList = "event_id", unique = true),
        @Index(name = "idx_aggregate", columnList = "aggregate_type, aggregate_id"),
        @Index(name = "idx_processed_at", columnList = "processed_at")
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventInbox extends BaseEntity {

    @Column(name = "event_id", nullable = false, length = 50)
    private String eventId;  // Outbox의 ID (멱등키)

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;  // ORDER, PRODUCT, LIKE, PAYMENT

    @Column(name = "aggregate_id", nullable = false, length = 50)
    private String aggregateId;  // orderId, productId 등

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;  // OrderCreatedEvent, LikeCreatedEvent 등

    @Column(name = "processed_at", nullable = false)
    private java.time.ZonedDateTime processedAt;

    @Builder
    private EventInbox(String eventId, String aggregateType, String aggregateId,
                       String eventType, java.time.ZonedDateTime processedAt) {
        this.eventId = eventId;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.processedAt = processedAt != null ? processedAt : java.time.ZonedDateTime.now();
    }
}
