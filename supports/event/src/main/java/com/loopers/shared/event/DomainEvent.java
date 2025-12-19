package com.loopers.shared.event;

import java.time.LocalDateTime;

/**
 * 모든 도메인 이벤트의 표준 인터페이스
 * 이벤트 발행 시 파티션 키와 발생 시각 정보를 제공합니다.
 */
public interface DomainEvent {
    /**
     * 이벤트의 파티션 키를 반환합니다.
     * Kafka 등 메시지 브로커에서 파티션 분산에 사용됩니다.
     * 
     * @return 파티션 키 (예: orderId, productId 등)
     */
    String getPartitionKey();
    
    /**
     * 이벤트가 발생한 시각을 반환합니다.
     * 
     * @return 이벤트 발생 시각
     */
    LocalDateTime getOccurredAt();
}

