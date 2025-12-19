package com.loopers.shared.event;

/**
 * 도메인 이벤트 타입과 Kafka 토픽 간의 매핑을 관리하는 인터페이스
 * 각 애플리케이션에서 구현하여 자신의 도메인 이벤트에 맞는 토픽 매핑을 제공합니다.
 */
public interface EventTopicRegistry {
    /**
     * 이벤트 타입에 해당하는 Kafka 토픽을 반환합니다.
     * 
     * @param eventClass 이벤트 클래스
     * @return Kafka 토픽명
     * @throws IllegalArgumentException 토픽이 등록되지 않은 이벤트 타입인 경우
     */
    String getTopic(Class<? extends DomainEvent> eventClass);
}

