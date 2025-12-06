package com.loopers.infrastructure.order;

import com.loopers.domain.order.Payment;
import com.loopers.domain.order.PaymentRepository;
import com.loopers.domain.order.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {

    private final PaymentJpaRepository paymentJpaRepository;

    @Override
    public Payment save(Payment payment) {
        return paymentJpaRepository.save(payment);
    }

    @Override
    public Optional<Payment> findById(Long id) {
        return paymentJpaRepository.findById(id);
    }

    @Override
    public Optional<Payment> findByOrderId(Long orderId) {
        return paymentJpaRepository.findByOrderId(orderId);
    }

    @Override
    public List<Payment> findByUserId(String userId) {
        return paymentJpaRepository.findByUserId(userId);
    }

    @Override
    public List<Payment> findByUserIdAndStatus(String userId, PaymentStatus status) {
        return paymentJpaRepository.findByUserIdAndStatus(userId, status);
    }

    @Override
    public Optional<Payment> findByPgTransactionId(String pgTransactionId) {
        return paymentJpaRepository.findByPgTransactionId(pgTransactionId);
    }
}
