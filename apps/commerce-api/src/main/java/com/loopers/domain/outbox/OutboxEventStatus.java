package com.loopers.domain.outbox;

public enum OutboxEventStatus {
    PENDING,    // 처리 대기
    PROCESSED,  // 처리 완료
    FAILED      // 처리 실패
}