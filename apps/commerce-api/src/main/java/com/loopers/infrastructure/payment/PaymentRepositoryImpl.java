package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentRepository;
import com.loopers.domain.payment.PaymentStatus;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class PaymentRepositoryImpl implements PaymentRepository {

    private final PaymentJpaRepository paymentJpaRepository;

    @Override
    public void savePayment(Payment payment) {
        paymentJpaRepository.save(payment);
    }

    @Override
    public Optional<Payment> findByOrderKey(String orderKey) {
        return paymentJpaRepository.findByOrderKey(orderKey);
    }

    @Override
    public List<Payment> getPendingPaymentsCreatedBefore(ZonedDateTime before) {
        return paymentJpaRepository.findPendingPaymentsCreatedBefore(PaymentStatus.PENDING, before);
    }
}

