package com.loopers.domain.event;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 도메인별 Outbox Event의 공통 필드와 메서드를 담은 추상 클래스
 * @MappedSuperclass를 사용하여 상속받는 엔티티들이 공통 필드를 가지도록 함
 */
@MappedSuperclass
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseOutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String eventId;       // UUID - Consumer 측 멱등성 보장용

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
    private int retryCount = 0;   // 재시도 횟수

    @Column(nullable = false)
    private int maxRetries = 3;   // 최대 재시도 횟수

    @Column(columnDefinition = "TEXT")
    private String lastError;     // 마지막 에러 메시지

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime publishedAt;

    private LocalDateTime nextRetryAt; // 재시도 예정 시간 (지수 백오프)

    protected BaseOutboxEvent(String eventId, String aggregateId, String type, String payload, String topic) {
        this.eventId = eventId;
        this.aggregateId = aggregateId;
        this.type = type;
        this.payload = payload;
        this.topic = topic;
        this.status = OutboxStatus.PENDING;
        this.retryCount = 0;
        this.maxRetries = 3;
        this.createdAt = LocalDateTime.now();
        this.nextRetryAt = LocalDateTime.now();
    }

    public void markAsPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    public void markAsFailed(String error) {
        this.status = OutboxStatus.FAILED;
        this.lastError = error;
        this.retryCount++;
        updateNextRetryAt();
    }

    public void markAsDeadLetter(String error) {
        this.status = OutboxStatus.DEAD_LETTER;
        this.lastError = error;
    }

    public boolean shouldRetry() {
        return retryCount < maxRetries && status == OutboxStatus.FAILED;
    }

    public boolean isReadyForRetry() {
        return status == OutboxStatus.FAILED 
            && nextRetryAt != null 
            && LocalDateTime.now().isAfter(nextRetryAt);
    }

    private void updateNextRetryAt() {
        // 지수 백오프: 2^retryCount 초 (최대 300초 = 5분)
        long delaySeconds = Math.min((long) Math.pow(2, retryCount), 300);
        this.nextRetryAt = LocalDateTime.now().plusSeconds(delaySeconds);
    }
}

