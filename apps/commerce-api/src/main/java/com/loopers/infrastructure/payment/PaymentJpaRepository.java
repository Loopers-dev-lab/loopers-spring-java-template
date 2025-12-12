package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Payment JPA Repository
 */
public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, Long> {

    /**
     * PG 트랜잭션 키로 조회
     */
    Optional<PaymentEntity> findByPgTransactionKey(String pgTransactionKey);

    /**
     * 주문 ID로 조회
     */
    List<PaymentEntity> findByOrderId(Long orderId);

    /**
     * 사용자 ID와 주문 ID로 조회
     */
    List<PaymentEntity> findByUserIdAndOrderId(String userId, Long orderId);

    /**
     * 상태로 조회
     */
    List<PaymentEntity> findByStatus(PaymentStatus status);
}
