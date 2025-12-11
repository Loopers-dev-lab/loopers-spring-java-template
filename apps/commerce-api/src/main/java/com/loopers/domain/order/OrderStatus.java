package com.loopers.domain.order;

public enum OrderStatus {
    PENDING,         // 대기 (주문 생성됨, 결제 대기 중)
    PAID,            // 결제 완료
    PAYMENT_FAILED,  // 결제 실패
    COMPLETED,       // 완료 (배송 완료 등)
    CANCELED         // 취소
}
