package com.loopers.infrastructure.payment;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.loopers.domain.payment.PaymentEntity;
import com.loopers.domain.payment.PaymentStatus;

/**
 * @author hyunjikoh
 * @since 2025. 12. 3.
 */
public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, Long> {
    Optional<PaymentEntity> findByOrderNumber(Long orderNumber);

    Optional<PaymentEntity> findByTransactionKey(String id);

    /**
     * 특정 상태이면서 특정 시간 이전에 요청된 결제 건 조회
     *
     * @param status      결제 상태
     * @param requestedAt 요청 시간
     * @return 조건에 맞는 결제 건 목록
     */
    List<PaymentEntity> findByPaymentStatusAndRequestedAtBefore(
            PaymentStatus status,
            ZonedDateTime requestedAt
    );
}
