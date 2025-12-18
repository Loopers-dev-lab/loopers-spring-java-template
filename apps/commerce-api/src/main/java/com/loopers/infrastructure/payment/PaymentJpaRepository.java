package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentJpaRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByIdAndPaymentStatus(Long id, PaymentStatus paymentStatus);
}
