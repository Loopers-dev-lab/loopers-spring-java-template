package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentRepository;
import com.loopers.domain.payment.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class PaymentRepositoryImpl implements PaymentRepository {
    private final PaymentJpaRepository paymentJpaRepository;

    @Override
    public Optional<Payment> findByPgTransactionId(String pgTransactionId) {
        return paymentJpaRepository.findByPgTransactionId(pgTransactionId);
    }

    @Override
    public Payment save(Payment payment) {
        return paymentJpaRepository.save(payment);
    }

    @Override
    public List<Payment> findProcessingPaymentsForStatusCheck(LocalDateTime thresholdTime, int maxCheckCount) {
        return paymentJpaRepository.findByStatusAndLastStatusCheckAtBeforeOrLastStatusCheckAtIsNullAndStatusCheckCountLessThan(
                PaymentStatus.PROCESSING,
                thresholdTime,
                maxCheckCount
        );
    }

    @Override
    public Optional<Payment> findByPaymentId(String paymentId) {
        return paymentJpaRepository.findByPaymentId(paymentId);
    }
}
