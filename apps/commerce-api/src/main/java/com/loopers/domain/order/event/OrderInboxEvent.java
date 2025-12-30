package com.loopers.domain.order.event;

import com.loopers.domain.event.BaseInboxEvent;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "inbox_order", indexes = {
        @Index(name = "idx_inbox_order_event_id", columnList = "eventId", unique = true),
        @Index(name = "idx_inbox_order_processed_at", columnList = "processedAt")
})
public class OrderInboxEvent extends BaseInboxEvent {

    @Builder
    public OrderInboxEvent(String eventId, String aggregateId, String type, String topic) {
        super(eventId, aggregateId, type, topic);
    }
}

