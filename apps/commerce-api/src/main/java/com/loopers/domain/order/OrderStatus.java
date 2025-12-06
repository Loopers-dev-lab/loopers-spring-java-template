package com.loopers.domain.order;

public enum OrderStatus {
    PENDING,      // 주문 요청
    PAYING,       // 결제 진행 중 (결제 처리 중)
    CONFIRMED,    // 주문 완료 (결제 완료)
    FAILED,       // 결제 실패
    CANCELLED     // 주문 취소
}
