package com.loopers.domain.payment;

public interface PaymentGateway {

    /**
     * 외부 결제 시스템을 통해 결제를 처리합니다.
     *
     * @param userId 사용자 ID
     * @param payment 결제 정보
     * @param callbackUrl 결제 완료 후 콜백 URL
     * @return 결제 처리 결과
     */
    PaymentResult processPayment(String userId, Payment payment, String callbackUrl);

    /**
     * PG사에 결제 상태를 조회합니다.
     *
     * @param pgTransactionId PG 거래 ID
     * @return 결제 상태 조회 결과
     */
    PaymentResult checkPaymentStatus(String pgTransactionId);
}