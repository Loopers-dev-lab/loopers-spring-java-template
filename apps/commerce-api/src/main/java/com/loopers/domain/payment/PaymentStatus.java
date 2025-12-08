package com.loopers.domain.payment;

public enum PaymentStatus {
    PENDING,      // 결제 생성됨 (PG 요청 전)
    PROCESSING,   // PG에 요청됨 (결제 처리 중)
    SUCCESS,      // 결제 성공 (콜백으로 확인됨)
    FAILED        // 결제 실패 (콜백으로 확인됨)
}
