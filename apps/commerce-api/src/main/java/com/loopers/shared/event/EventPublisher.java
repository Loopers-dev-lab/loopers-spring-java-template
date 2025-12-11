package com.loopers.shared.event;

/**
 * Kafka로 쉽게 전환 가능한 이벤트 발행 추상화 인터페이스
 * 현재는 ApplicationEventPublisher 사용, 나중에 Kafka로 교체 가능
 */
public interface EventPublisher {
    /**
     * 이벤트 발행
     * @param topic 토픽/이벤트 타입 (토픽명으로 사용)
     * @param key 파티션 키 (파티션 키로 사용)
     * @param event 이벤트 객체
     */
    <T> void publish(String topic, String key, T event);
}

