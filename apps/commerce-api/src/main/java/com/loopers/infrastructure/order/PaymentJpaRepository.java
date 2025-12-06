package com.loopers.infrastructure.order;

import com.loopers.domain.order.Payment;
import com.loopers.domain.order.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentJpaRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    List<Payment> findByUserId(String userId);

    List<Payment> findByUserIdAndStatus(String userId, PaymentStatus status);

    Optional<Payment> findByPgTransactionId(String pgTransactionId);
}
