package com.loopers.event.outbox;

import com.loopers.shared.event.DomainEvent;

/**
 * Outbox 패턴을 사용한 이벤트 발행 인터페이스
 * 애플리케이션 모듈에서 구현하여 도메인별 Outbox 테이블에 이벤트를 저장합니다.
 */
public interface OutboxEventPublisher {
    /**
     * 이벤트를 Outbox 테이블에 저장합니다.
     * 실제 Kafka 발행은 별도의 Processor가 배치로 처리합니다.
     *
     * @param topic Kafka 토픽명
     * @param key 파티션 키
     * @param event 도메인 이벤트
     */
    void publish(String topic, String key, DomainEvent event);
}

