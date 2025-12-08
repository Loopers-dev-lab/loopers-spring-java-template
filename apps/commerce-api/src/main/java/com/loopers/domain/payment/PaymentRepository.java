package com.loopers.domain.payment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository {

    /**
     * PG Transaction ID로 Payment 조회
     *
     * @param pgTransactionId PG사에서 발급한 거래 키
     * @return Payment 엔티티 (Optional)
     */
    Optional<Payment> findByPgTransactionId(String pgTransactionId);

    /**
     * Payment 저장
     *
     * @param payment Payment 엔티티
     * @return 저장된 Payment
     */
    Payment save(Payment payment);

    /**
     * PROCESSING 상태인 Payment 목록 조회 (상태 확인 대상)
     * - PROCESSING 상태
     * - 마지막 확인 시간이 기준 시간 이전 (또는 확인한 적 없음)
     * - 최대 확인 횟수 미만
     *
     * @param thresholdTime 기준 시간 (이 시간 이전에 마지막으로 확인된 결제만 조회)
     * @param maxCheckCount 최대 확인 횟수
     * @return PROCESSING 상태인 Payment 목록
     */
    List<Payment> findProcessingPaymentsForStatusCheck(LocalDateTime thresholdTime, int maxCheckCount);

    Optional<Payment> findByPaymentId(String paymentId);
}
