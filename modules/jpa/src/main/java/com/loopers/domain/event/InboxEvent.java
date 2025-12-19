package com.loopers.domain.event;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "inbox_events", uniqueConstraints = {
        @UniqueConstraint(name = "uk_inbox_events_message_id", columnNames = {"messageId"})
})
public class InboxEvent extends BaseEntity {

    @Column(nullable = false)
    private String messageId;

    @Column(nullable = false)
    private LocalDateTime processedAt;

    @Column(nullable = false)
    private LocalDateTime eventTimestamp;

    @Builder
    public InboxEvent(String messageId, LocalDateTime eventTimestamp) {
        this.messageId = messageId;
        this.processedAt = LocalDateTime.now();
        this.eventTimestamp = eventTimestamp;
    }
}
