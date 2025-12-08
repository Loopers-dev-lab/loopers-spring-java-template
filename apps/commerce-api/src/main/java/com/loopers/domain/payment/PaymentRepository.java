package com.loopers.domain.payment;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

/**
 * @author hyunjikoh
 * @since 2025. 12. 3.
 */
public interface PaymentRepository {
    PaymentEntity save(PaymentEntity payment);

    Optional<PaymentEntity> findByOrderNumber(Long orderNumber);

    Optional<PaymentEntity> findByTransactionKey(String transactionKey);

    /**
     * PENDING 상태로 특정 시간 이전에 생성된 결제 건 조회
     *
     * @param threshold 기준 시간
     * @return PENDING 상태의 오래된 결제 건 목록
     */
    List<PaymentEntity> findPendingPaymentsOlderThan(ZonedDateTime threshold);
}
