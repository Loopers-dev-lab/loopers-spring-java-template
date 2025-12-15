package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {
    private final PaymentJpaRepository paymentJpaRepository;

    @Override
    public Optional<Payment> findByOrderId(Long orderId) {
        return paymentJpaRepository.findByOrderId(orderId);
    }

    public Payment savePayment(Payment payment) {
        return paymentJpaRepository.save(payment);
    }

    public void saveIdempotencyKeyByOrderId(Long orderId, String idempotencyKey) {
        paymentJpaRepository.saveIdempotencyKeyByOrderId(orderId, idempotencyKey);
    }
}
