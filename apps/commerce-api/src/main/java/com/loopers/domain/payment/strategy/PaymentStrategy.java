package com.loopers.domain.payment.strategy;

import com.loopers.domain.payment.PaymentDto;

import java.math.BigDecimal;

/**
 * 결제 전략 인터페이스
 * Strategy Pattern을 사용하여 다양한 결제 방법을 지원
 */
public interface PaymentStrategy {
    
    /**
     * 결제 처리
     * @param orderId 주문 ID
     * @param userId 사용자 ID
     * @param amount 결제 금액
     * @return 결제 결과
     */
    PaymentResult processPayment(Long orderId, Long userId, BigDecimal amount);
    
    /**
     * 이 전략이 지원하는 결제 방법
     * @return 결제 방법
     */
    PaymentDto.PaymentMethod getPaymentMethod();
    
    /**
     * 결제 결과
     * @param success 결제 성공 여부
     * @param transactionKey 거래 키 (PG 결제의 경우 PG에서 발급, 포인트 결제의 경우 자체 생성)
     * @param status 결제 상태
     * @param reason 실패 사유 (성공 시 null)
     */
    record PaymentResult(
        boolean success,
        String transactionKey,
        PaymentDto.PaymentStatus status,
        String reason
    ) {}
}
