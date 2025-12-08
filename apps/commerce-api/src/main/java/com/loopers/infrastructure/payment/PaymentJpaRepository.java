package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentJpaRepository extends JpaRepository<Payment, String> {

    /**
     * PG Transaction ID로 Payment 조회
     *
     * @param pgTransactionId PG사에서 발급한 거래 키
     * @return Payment 엔티티 (Optional)
     */
    Optional<Payment> findByPgTransactionId(String pgTransactionId);

    /**
     * PROCESSING 상태인 결제 중 상태 확인 대상 조회
     * - PROCESSING 상태
     * - 마지막 확인 시간이 기준 시간 이전이거나 확인한 적 없음
     * - 최대 확인 횟수 미만
     *
     * @param status 결제 상태
     * @param thresholdTime 기준 시간
     * @param maxCheckCount 최대 확인 횟수
     * @return 상태 확인 대상 Payment 목록
     */
    @Query("SELECT p FROM Payment p WHERE p.status = :status " +
            "AND (p.lastStatusCheckAt < :thresholdTime OR p.lastStatusCheckAt IS NULL) " +
            "AND p.statusCheckCount < :maxCheckCount")
    List<Payment> findByStatusAndLastStatusCheckAtBeforeOrLastStatusCheckAtIsNullAndStatusCheckCountLessThan(
            @Param("status") PaymentStatus status,
            @Param("thresholdTime") LocalDateTime thresholdTime,
            @Param("maxCheckCount") Integer maxCheckCount
    );

    Optional<Payment> findByPaymentId(String paymentId);
}
