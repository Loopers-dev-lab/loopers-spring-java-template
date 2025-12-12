package com.loopers.domain.payment;

/**
 * 결제 상태
 * - PENDING: 결제 대기 (PG 처리 중)
 * - SUCCESS: 결제 성공
 * - FAILED: 결제 실패
 */
public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED
}
