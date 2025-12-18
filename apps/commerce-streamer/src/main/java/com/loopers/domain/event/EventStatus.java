package com.loopers.domain.event;

public enum EventStatus {
    PENDING,    // 처리 대기
    PROCESSING, // 처리 중
    COMPLETED,  // 처리 완료
    FAILED,     // 처리 실패 (DLQ)
    SKIPPED     // 중복으로 스킵
}