package com.loopers.domain.like.event;

import com.loopers.domain.event.BaseOutboxEvent;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "outbox_like", indexes = {
        @Index(name = "idx_outbox_like_created_at_status", columnList = "createdAt, status"),
        @Index(name = "idx_outbox_like_event_id", columnList = "eventId", unique = true),
        @Index(name = "idx_outbox_like_status_next_retry", columnList = "status, nextRetryAt")
})
public class LikeOutboxEvent extends BaseOutboxEvent {

    @Builder
    public LikeOutboxEvent(String eventId, String aggregateId, String type, String payload, String topic) {
        super(eventId, aggregateId, type, payload, topic);
    }
}

