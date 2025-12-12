package com.loopers.domain.payment;

import java.util.List;
import java.util.Optional;

/**
 * Payment 레포지토리 인터페이스
 * - Domain 레이어에서 정의
 * - Infrastructure 레이어에서 구현
 */
public interface PaymentRepository {

    /**
     * 결제 저장
     */
    Payment save(Payment payment);

    /**
     * ID로 결제 조회
     */
    Optional<Payment> findById(Long id);

    /**
     * PG 트랜잭션 키로 결제 조회
     */
    Optional<Payment> findByPgTransactionKey(String pgTransactionKey);

    /**
     * 주문 ID로 결제 목록 조회
     */
    List<Payment> findByOrderId(Long orderId);

    /**
     * 사용자 ID와 주문 ID로 결제 목록 조회
     */
    List<Payment> findByUserIdAndOrderId(String userId, Long orderId);

    /**
     * PENDING 상태인 결제 목록 조회 (스케줄러용)
     */
    List<Payment> findPendingPayments();
}
