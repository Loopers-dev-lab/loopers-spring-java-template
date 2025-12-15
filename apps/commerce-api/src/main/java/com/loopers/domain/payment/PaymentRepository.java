package com.loopers.domain.payment;

import java.util.Optional;

public interface PaymentRepository {
    Optional<Payment> findByOrderId(Long orderId);
    Payment savePayment(Payment payment);
    void saveIdempotencyKeyByOrderId(Long orderId, String idempotencyKey);
}
