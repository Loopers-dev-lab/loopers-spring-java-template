package com.loopers.infrastructure.outbox;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

/**
 * Transactional Outbox Pattern
 * - 이벤트를 DB에 먼저 저장하여 트랜잭션 원자성 보장
 * - 별도 Poller가 PENDING 이벤트를 Kafka로 발행
 */
@Entity
@Table(name = "outbox_event", indexes = {
    @Index(name = "idx_status_created", columnList = "status, created_at"),
    @Index(name = "idx_event_type_key", columnList = "event_type, partition_key")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent extends BaseEntity {

    /**
     * Kafka Partition Key
     * - 같은 엔티티의 이벤트는 같은 파티션으로 전송되어 순서 보장
     * - 예: productId "1", orderNo "ORD-20250101-001"
     */
    @Column(name = "partition_key", nullable = false, length = 100)
    private String partitionKey;

    /**
     * 이벤트 타입
     * - 예: PRODUCT_LIKED, ORDER_PLACED, PAYMENT_COMPLETED
     */
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    /**
     * 이벤트 Payload (JSON)
     * - Application Event 객체를 JSON으로 직렬화한 값
     * - 예: OrderPlacedEvent, ProductLikedEvent 등
     */
    @Column(name = "payload", columnDefinition = "JSON", nullable = false)
    private String payload;

    /**
     * 발행 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboxStatus status = OutboxStatus.PENDING;

    /**
     * Kafka 발행 완료 시각
     */
    @Column(name = "published_at")
    private ZonedDateTime publishedAt;

    /**
     * 재시도 횟수
     */
    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    /**
     * 실패 원인
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Outbox 이벤트 생성
     */
    public static OutboxEvent create(
            String partitionKey,
            String eventType,
            String payload
    ) {
        OutboxEvent event = new OutboxEvent();
        event.partitionKey = partitionKey;
        event.eventType = eventType;
        event.payload = payload;
        event.status = OutboxStatus.PENDING;
        event.retryCount = 0;
        return event;
    }

    /**
     * Kafka 발행 성공 처리
     */
    public void markAsPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = ZonedDateTime.now();
    }

    /**
     * Kafka 발행 실패 처리
     */
    public void markAsFailed(String errorMessage) {
        this.status = OutboxStatus.FAILED;
        this.retryCount++;
        this.errorMessage = errorMessage;
    }

    /**
     * 재시도 가능 여부 확인
     * @param maxRetryCount 최대 재시도 횟수
     */
    public boolean canRetry(int maxRetryCount) {
        return this.status == OutboxStatus.FAILED && this.retryCount < maxRetryCount;
    }

    /**
     * 발행 상태
     */
    public enum OutboxStatus {
        PENDING,    // 발행 대기 중
        PUBLISHED,  // 발행 완료
        FAILED      // 발행 실패
    }
}
