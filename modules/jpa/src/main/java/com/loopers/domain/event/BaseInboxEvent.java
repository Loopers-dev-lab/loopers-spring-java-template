package com.loopers.domain.event;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 도메인별 Inbox Event의 공통 필드와 메서드를 담은 추상 클래스
 * @MappedSuperclass를 사용하여 상속받는 엔티티들이 공통 필드를 가지도록 함
 * 
 * Inbox 패턴: Consumer가 처리한 이벤트를 기록하여 멱등성을 보장합니다.
 */
@MappedSuperclass
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseInboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String eventId;       // UUID - 멱등성 보장용

    @Column(nullable = false)
    private String aggregateId;   // e.g., OrderId, ProductId

    @Column(nullable = false)
    private String type;          // e.g., "OrderCreated", "ProductUpdated"

    @Column(nullable = false)
    private String topic;          // Kafka topic

    @Column(nullable = false)
    private LocalDateTime processedAt;  // 처리 완료 시각

    protected BaseInboxEvent(String eventId, String aggregateId, String type, String topic) {
        this.eventId = eventId;
        this.aggregateId = aggregateId;
        this.type = type;
        this.topic = topic;
        this.processedAt = LocalDateTime.now();
    }
}

