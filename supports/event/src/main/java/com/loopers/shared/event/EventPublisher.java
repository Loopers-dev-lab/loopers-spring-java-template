package com.loopers.shared.event;

/**
 * 이벤트 발행을 위한 추상화 인터페이스
 * 다양한 메시지 브로커(Kafka, RabbitMQ 등) 또는 내부 이벤트 시스템으로의 전환을 쉽게 할 수 있습니다.
 */
public interface EventPublisher {
    /**
     * 도메인 이벤트를 발행합니다.
     * 
     * @param event 발행할 도메인 이벤트
     */
    void publish(DomainEvent event);
}

