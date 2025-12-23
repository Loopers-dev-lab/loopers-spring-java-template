package com.loopers.domain.outbox;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;

@Entity
@Table(name = "outbox")
@Getter
public class Outbox extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String topic;

    @Column(nullable = false, length = 100)
    private String partitionKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(length = 500)
    private String errorMessage;

    @Column(nullable = false)
    private Integer retryCount;

    @Builder
    private Outbox(String topic, String partitionKey, String payload, OutboxStatus status, Integer retryCount) {
        this.topic = topic;
        this.partitionKey = partitionKey;
        this.payload = payload;
        this.status = status;
        this.retryCount = retryCount != null ? retryCount : 0;
    }

    public Outbox() {
        this.retryCount = 0;
    }

    public static Outbox create(String topic, String partitionKey, String payload) {
        return Outbox.builder()
                .topic(topic)
                .partitionKey(partitionKey)
                .payload(payload)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();
    }

    public boolean hasExceededMaxRetries(int maxRetries) {
        return this.retryCount >= maxRetries;
    }
}

