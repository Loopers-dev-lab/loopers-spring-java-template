package com.loopers.domain.order;

public enum OrderStatus {
    PENDING,      // 주문 요청
    CONFIRMED,    // 주문 완료 (재고/포인트 차감 완료)
    CANCELLED     // 주문 취소
}
