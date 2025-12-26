package com.loopers.domain.event;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

/**
 * 이벤트 처리 이력 (멱등성 보장)
 * - 중복 이벤트 처리 방지를 위한 테이블
 * - event_id를 PK로 사용하여 같은 이벤트는 한 번만 처리
 */
@Entity
@Table(name = "event_handled", indexes = {
    @Index(name = "idx_event_type_key", columnList = "event_type, partition_key"),
    @Index(name = "idx_handled_at", columnList = "handled_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventHandled {

    /**
     * 이벤트 ID (Outbox Event의 ID)
     * - Kafka 메시지의 eventId와 동일
     */
    @Id
    @Column(name = "event_id", length = 100)
    private String eventId;

    /**
     * 이벤트 타입
     */
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    /**
     * Partition Key (디버깅용)
     */
    @Column(name = "partition_key", nullable = false, length = 100)
    private String partitionKey;

    /**
     * 처리 완료 시각
     */
    @Column(name = "handled_at", nullable = false)
    private ZonedDateTime handledAt;

    /**
     * 이벤트 처리 완료 기록
     */
    public static EventHandled create(String eventId, String eventType, String partitionKey) {
        EventHandled handled = new EventHandled();
        handled.eventId = eventId;
        handled.eventType = eventType;
        handled.partitionKey = partitionKey;
        handled.handledAt = ZonedDateTime.now();
        return handled;
    }
}
