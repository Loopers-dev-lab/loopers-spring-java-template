package com.loopers.domain.event.outbox;

import java.time.ZonedDateTime;
import java.util.Objects;

import lombok.Getter;

import jakarta.persistence.*;

@Entity
@Getter
@Table(name = "outbox_event")
public class OutboxEventEntity {

    @Id
    @Column(name = "event_id", length = 36, nullable = false)
    private String eventId;

    @Column(name = "topic", nullable = false, length = 200)
    private String topic;

    @Column(name = "message_key", nullable = false, length = 200)
    private String messageKey;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "version", nullable = false, length = 20)
    private String version;

    @Column(name = "occurred_at_epoch_millis", nullable = false)
    private long occurredAtEpochMillis;

    @Lob
    @Column(name = "payload_json", nullable = false)
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboxStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;

    @Column(name = "published_at")
    private ZonedDateTime publishedAt;

    protected OutboxEventEntity() {
    }

    private OutboxEventEntity(
            final String eventId,
            final String topic,
            final String messageKey,
            final String eventType,
            final String version,
            final long occurredAtEpochMillis,
            final String payloadJson
    ) {
        // 유효성 검증
        Objects.requireNonNull(eventId, "이벤트 ID는 필수입니다.");
        if (eventId.trim().isEmpty() || eventId.length() != 36) {
            throw new IllegalArgumentException("이벤트 ID는 36자리 UUID 형식이어야 합니다.");
        }

        Objects.requireNonNull(topic, "토픽은 필수입니다.");
        if (topic.trim().isEmpty() || topic.length() > 200) {
            throw new IllegalArgumentException("토픽은 필수이며 200자를 초과할 수 없습니다.");
        }

        Objects.requireNonNull(messageKey, "메시지 키는 필수입니다.");
        if (messageKey.trim().isEmpty() || messageKey.length() > 200) {
            throw new IllegalArgumentException("메시지 키는 필수이며 200자를 초과할 수 없습니다.");
        }

        Objects.requireNonNull(eventType, "이벤트 타입은 필수입니다.");
        if (eventType.trim().isEmpty() || eventType.length() > 100) {
            throw new IllegalArgumentException("이벤트 타입은 필수이며 100자를 초과할 수 없습니다.");
        }

        Objects.requireNonNull(version, "버전은 필수입니다.");
        if (version.trim().isEmpty() || version.length() > 20) {
            throw new IllegalArgumentException("버전은 필수이며 20자를 초과할 수 없습니다.");
        }

        if (occurredAtEpochMillis <= 0) {
            throw new IllegalArgumentException("발생 시간은 0보다 커야 합니다.");
        }

        Objects.requireNonNull(payloadJson, "페이로드 JSON은 필수입니다.");
        if (payloadJson.trim().isEmpty()) {
            throw new IllegalArgumentException("페이로드 JSON은 필수입니다.");
        }

        this.eventId = eventId;
        this.topic = topic;
        this.messageKey = messageKey;
        this.eventType = eventType;
        this.version = version;
        this.occurredAtEpochMillis = occurredAtEpochMillis;
        this.payloadJson = payloadJson;

        this.status = OutboxStatus.READY;
        this.retryCount = 0;
        this.createdAt = ZonedDateTime.now();
    }

    public static OutboxEventEntity ready(
            final String eventId,
            final String topic,
            final String messageKey,
            final String eventType,
            final String version,
            final long occurredAtEpochMillis,
            final String payloadJson
    ) {
        return new OutboxEventEntity(eventId, topic, messageKey, eventType, version, occurredAtEpochMillis, payloadJson);
    }

    public void markSent() {
        this.status = OutboxStatus.SENT;
        this.publishedAt = ZonedDateTime.now();
    }

    public void markFailed() {
        this.status = OutboxStatus.FAILED;
        this.retryCount += 1;
    }

}
