package com.loopers.domain.ranking;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 랭킹 이벤트 로그 엔티티
 * 모든 랭킹 관련 이벤트를 Raw 데이터로 저장하여 Source of Truth 역할을 수행
 */
@Entity
@Table(name = "ranking_event_log", indexes = {
    @Index(name = "idx_event_id", columnList = "event_id", unique = true),
    @Index(name = "idx_product_id_occurred_at", columnList = "product_id, occurred_at"),
    @Index(name = "idx_occurred_at", columnList = "occurred_at")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class RankingEventLog extends BaseEntity {

    @Column(name = "event_id", nullable = false, unique = true, length = 36)
    private String eventId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private RankingEventType eventType;

    @Column(name = "score", nullable = false)
    private Double score;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Builder
    private RankingEventLog(String eventId, Long productId, RankingEventType eventType, Double score, LocalDateTime occurredAt) {
        this.eventId = eventId;
        this.productId = productId;
        this.eventType = eventType;
        this.score = score;
        this.occurredAt = occurredAt != null ? occurredAt : LocalDateTime.now();
        this.guard();
    }

    @Override
    protected void guard() {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId는 필수입니다");
        }
        if (productId == null) {
            throw new IllegalArgumentException("productId는 필수입니다");
        }
        if (eventType == null) {
            throw new IllegalArgumentException("eventType은 필수입니다");
        }
        if (score == null) {
            throw new IllegalArgumentException("score는 필수입니다");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("occurredAt은 필수입니다");
        }
    }
}

