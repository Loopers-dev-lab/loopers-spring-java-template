package com.loopers.domain.payment;

import java.util.Optional;

public interface PaymentRepository {
    Payment save(Payment payment);

    Optional<Payment> findByIdAndPaymentStatus(Long paymentId, PaymentStatus paymentStatus);

    Optional<Payment> findById(Long paymentId);
}
