package com.loopers.domain.order;

/**
 * 주문 상태
 * - PENDING: 주문 대기
 * - COMPLETED: 주문 완료
 * - CANCELLED: 주문 취소
 */
public enum OrderStatus {
    PENDING,
    COMPLETED,
    CANCELLED
}
