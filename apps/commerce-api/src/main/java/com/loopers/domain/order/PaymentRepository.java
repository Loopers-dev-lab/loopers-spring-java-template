package com.loopers.domain.order;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(Long id);

    Optional<Payment> findByOrderId(Long orderId);

    List<Payment> findByUserId(String userId);

    List<Payment> findByUserIdAndStatus(String userId, PaymentStatus status);

    Optional<Payment> findByPgTransactionId(String pgTransactionId);
}
