package com.loopers.domain.payment;

public enum PaymentStatus {
    PENDING,       // 결제 대기
    COMPLETED,     // 결제 완료
    FAILED,        // 결제 실패
    CANCELLED,     // 결제 취소
    REFUNDED       // 환불됨
}

