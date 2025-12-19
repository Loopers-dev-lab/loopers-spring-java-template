package com.loopers.domain.event;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 처리 실패한 이벤트를 저장하는 Dead Letter Queue 엔티티
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "dead_letter_events")
public class DeadLetterEvent extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String topic;

    @Column(nullable = false)
    private Integer partition;

    @Column(nullable = false)
    private Long offset;

    @Column(nullable = false, length = 500)
    private String eventId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false)
    private Integer retryCount;

    @Builder
    public DeadLetterEvent(String topic, Integer partition, Long offset, String eventId, 
                          String payload, String errorMessage, Integer retryCount) {
        this.topic = topic;
        this.partition = partition;
        this.offset = offset;
        this.eventId = eventId;
        this.payload = payload;
        this.errorMessage = errorMessage;
        this.retryCount = retryCount;
    }
}

