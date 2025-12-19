package com.loopers.domain.payment.event;

import com.loopers.domain.event.BaseOutboxEvent;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "outbox_payment", indexes = {
        @Index(name = "idx_outbox_payment_created_at_status", columnList = "createdAt, status"),
        @Index(name = "idx_outbox_payment_event_id", columnList = "eventId", unique = true),
        @Index(name = "idx_outbox_payment_status_next_retry", columnList = "status, nextRetryAt")
})
public class PaymentOutboxEvent extends BaseOutboxEvent {

    @Builder
    public PaymentOutboxEvent(String eventId, String aggregateId, String type, String payload, String topic) {
        super(eventId, aggregateId, type, payload, topic);
    }
}

