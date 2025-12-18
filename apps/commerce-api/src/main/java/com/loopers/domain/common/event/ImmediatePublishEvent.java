package com.loopers.domain.common.event;

/**
 * 이 인터페이스를 구현한 이벤트는 AFTER_COMMIT 시점에 즉시 Kafka로 발행
 * 실시간성이 중요한 이벤트(예: 주문 생성/완료)에 사용
 */
public interface ImmediatePublishEvent extends DomainEvent {
}
