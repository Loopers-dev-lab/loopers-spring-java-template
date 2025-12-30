package com.loopers.shared.event;

/**
 * 이벤트 핸들러 인터페이스
 * 비즈니스 로직을 인프라(Kafka, ApplicationEvent 등)로부터 분리하기 위한 추상화
 * 
 * @param <T> 처리할 도메인 이벤트 타입
 */
@FunctionalInterface
public interface EventHandler<T extends DomainEvent> {
    /**
     * 도메인 이벤트를 처리합니다.
     * 
     * @param event 처리할 도메인 이벤트
     */
    void handle(T event);
}

