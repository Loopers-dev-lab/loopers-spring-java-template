package com.loopers.domain.outbox;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Event Outbox 패턴 구현
 * - 이벤트를 DB에 먼저 저장하여 이벤트 손실 방지
 * - 별도 프로세스가 읽어서 실제 이벤트 발행
 * - At-least-once 전송 보장
 */
@Getter
@Entity
@Table(name = "event_outbox")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventOutbox extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String aggregateType; // ORDER, PAYMENT, LIKE

    @Column(nullable = false, length = 50)
    private String aggregateId; // orderId, paymentId

    @Column(nullable = false, length = 100)
    private String eventType; // OrderCreatedEvent, PaymentSuccessEvent

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload; // JSON

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(nullable = false)
    private Integer retryCount = 0;

    private LocalDateTime publishedAt;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Builder
    private EventOutbox(String aggregateType, String aggregateId, String eventType,
        String payload, OutboxStatus status) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = status != null ? status : OutboxStatus.PENDING;
        this.retryCount = 0;
    }

    public void markAsPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    public void markAsFailed(String errorMessage) {
        this.status = OutboxStatus.FAILED;
        this.errorMessage = errorMessage;
        this.retryCount++;
    }

    public boolean canRetry() {
        return this.retryCount < 3;
    }

    public boolean isPending() {
        return this.status == OutboxStatus.PENDING;
    }
}
