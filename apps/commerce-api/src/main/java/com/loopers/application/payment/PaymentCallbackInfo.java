package com.loopers.application.payment;

/**
 * 결제 콜백 처리 Command
 */
public record PaymentCallbackInfo(
        String transactionKey,  // PG 거래 키
        String status,          // 결제 상태 ("SUCCESS", "FAILED")
        String reason           // 실패 사유 (실패 시)
) {
}
