package com.loopers.domain.dlq;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Table(name = "dlq_message")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DlqMessage {

    @Id
    private String id;

    @Column(name = "original_topic", nullable = false)
    private String originalTopic;

    @Column(name = "partition_num")
    private Integer partitionNum;

    @Column(name = "offset_num")
    private Long offsetNum;

    @Column(name = "message_key")
    private String messageKey;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "retry_count")
    private int retryCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DlqStatus status;

    public static DlqMessage create(
            String originalTopic,
            Integer partitionNum,
            Long offsetNum,
            String messageKey,
            String payload,
            String errorMessage
    ) {
        DlqMessage dlqMessage = new DlqMessage();
        dlqMessage.id = UUID.randomUUID().toString();
        dlqMessage.originalTopic = originalTopic;
        dlqMessage.partitionNum = partitionNum;
        dlqMessage.offsetNum = offsetNum;
        dlqMessage.messageKey = messageKey;
        dlqMessage.payload = payload;
        dlqMessage.errorMessage = errorMessage;
        dlqMessage.createdAt = LocalDateTime.now();
        dlqMessage.retryCount = 0;
        dlqMessage.status = DlqStatus.PENDING;
        return dlqMessage;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    public void markAsResolved() {
        this.status = DlqStatus.RESOLVED;
        this.processedAt = LocalDateTime.now();
    }

    public void markAsAbandoned() {
        this.status = DlqStatus.ABANDONED;
        this.processedAt = LocalDateTime.now();
    }

    public boolean canRetry(int maxRetryCount) {
        return this.retryCount < maxRetryCount && this.status == DlqStatus.PENDING;
    }

    public enum DlqStatus {
        PENDING,
        RESOLVED,
        ABANDONED
    }
}
