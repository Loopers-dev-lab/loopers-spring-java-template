package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentRepository;
import com.loopers.domain.payment.PaymentStatus;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class PaymentRepositoryImpl implements PaymentRepository {
    private final PaymentJpaRepository paymentJpaRepository;

    @Override
    public Payment save(final Payment payment) {
        return paymentJpaRepository.save(payment);
    }

    @Override
    public Optional<Payment> findByIdAndPaymentStatus(final Long paymentId, final PaymentStatus paymentStatus) {
        return paymentJpaRepository.findByIdAndPaymentStatus(paymentId, paymentStatus);
    }

    @Override
    public Optional<Payment> findById(final Long paymentId) {
        return paymentJpaRepository.findById(paymentId);
    }
}
