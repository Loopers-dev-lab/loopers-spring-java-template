package com.loopers.domain.order;

public enum OrderStatus {
    PENDING,           // 주문 대기
    PAYMENT_FAILED,    // 결제 실패 (요구사항에 명시됨)
    CONFIRMED,         // 결제 완료/주문 확인
    SHIPPING,          // 배송 중
    DELIVERED,         // 배송 완료
    CANCELLED          // 취소됨
}

