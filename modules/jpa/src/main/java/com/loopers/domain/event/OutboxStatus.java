package com.loopers.domain.event;

/**
 * Outbox 이벤트의 상태를 나타내는 enum
 */
public enum OutboxStatus {
    /**
     * 발행 대기 중
     */
    PENDING,
    
    /**
     * 발행 완료
     */
    PUBLISHED,
    
    /**
     * 발행 실패 (재시도 가능)
     */
    FAILED,
    
    /**
     * Dead Letter (최대 재시도 횟수 초과)
     */
    DEAD_LETTER
}


