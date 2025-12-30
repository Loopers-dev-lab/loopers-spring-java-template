package com.loopers.domain.coupon.event;

import com.loopers.domain.event.BaseInboxEvent;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "inbox_coupon", indexes = {
        @Index(name = "idx_inbox_coupon_event_id", columnList = "eventId", unique = true),
        @Index(name = "idx_inbox_coupon_processed_at", columnList = "processedAt")
})
public class CouponInboxEvent extends BaseInboxEvent {

    @Builder
    public CouponInboxEvent(String eventId, String aggregateId, String type, String topic) {
        super(eventId, aggregateId, type, topic);
    }
}

