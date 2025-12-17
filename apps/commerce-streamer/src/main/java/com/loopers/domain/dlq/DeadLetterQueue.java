package com.loopers.domain.dlq;

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
 * Dead Letter Queue (DLQ)
 * - 처리 실패한 메시지를 저장
 * - 수동 재처리 또는 분석을 위한 테이블
 */
@Getter
@Entity
@Table(
    name = "dead_letter_queue",
    indexes = {
        @Index(name = "idx_failed_at", columnList = "failed_at"),
        @Index(name = "idx_event_id", columnList = "event_id"),
        @Index(name = "idx_topic", columnList = "original_topic")
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeadLetterQueue extends BaseEntity {

    @Column(name = "original_topic", nullable = false, length = 100)
    private String originalTopic;

    @Column(name = "partition_key", length = 100)
    private String partitionKey;

    @Column(name = "event_id", length = 50)
    private String eventId;

    @Column(name = "payload", columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "failed_at", nullable = false)
    private java.time.ZonedDateTime failedAt;

    @Builder
    private DeadLetterQueue(String originalTopic, String partitionKey, String eventId,
                            String payload, String errorMessage, Integer retryCount,
                            java.time.ZonedDateTime failedAt) {
        this.originalTopic = originalTopic;
        this.partitionKey = partitionKey;
        this.eventId = eventId;
        this.payload = payload;
        this.errorMessage = errorMessage;
        this.retryCount = retryCount != null ? retryCount : 0;
        this.failedAt = failedAt != null ? failedAt : java.time.ZonedDateTime.now();
    }
}
