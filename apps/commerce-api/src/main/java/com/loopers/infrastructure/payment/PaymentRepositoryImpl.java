package com.loopers.infrastructure.payment;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.loopers.domain.payment.PaymentEntity;
import com.loopers.domain.payment.PaymentRepository;
import com.loopers.domain.payment.PaymentStatus;

import lombok.RequiredArgsConstructor;

/**
 * @author hyunjikoh
 * @since 2025. 12. 3.
 */

@Component
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {
    private final PaymentJpaRepository paymentJpaRepository;

    @Override
    public PaymentEntity save(PaymentEntity payment) {
        return paymentJpaRepository.save(payment);
    }

    @Override
    public Optional<PaymentEntity> findByOrderNumber(Long orderNumber) {
        return paymentJpaRepository.findByOrderNumber(orderNumber);
    }

    @Override
    public Optional<PaymentEntity> findByTransactionKey(String id) {
        return paymentJpaRepository.findByTransactionKey(id);
    }

    @Override
    public List<PaymentEntity> findPendingPaymentsOlderThan(ZonedDateTime threshold) {
        return paymentJpaRepository.findByPaymentStatusAndRequestedAtBefore(
                PaymentStatus.PENDING,
                threshold
        );
    }
}
