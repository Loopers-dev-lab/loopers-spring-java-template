package com.loopers.core.infra.database.mysql.payment.impl;

import com.loopers.core.domain.error.NotFoundException;
import com.loopers.core.domain.order.vo.OrderKey;
import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.repository.PaymentRepository;
import com.loopers.core.domain.payment.type.PgPaymentStatus;
import com.loopers.core.domain.payment.vo.PaymentId;
import com.loopers.core.infra.database.mysql.payment.entity.PaymentEntity;
import com.loopers.core.infra.database.mysql.payment.repository.PaymentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {

    private final PaymentJpaRepository repository;

    @Override
    public Optional<Payment> findByWithLock(OrderKey orderKey, PgPaymentStatus status) {
        return repository.findByOrderKeyAndStatus(orderKey.value(), status.name())
                .map(PaymentEntity::to);
    }

    @Override
    public Payment getById(PaymentId paymentId) {
        return repository.findById(Long.parseLong(Objects.requireNonNull(paymentId.value())))
                .orElseThrow(() -> NotFoundException.withName("결제"))
                .to();
    }

    @Override
    public Payment save(Payment payment) {
        return repository.save(PaymentEntity.from(payment)).to();
    }

    @Override
    public Optional<Payment> findBy(OrderKey orderKey, PgPaymentStatus status) {
        return repository.findByOrderKeyAndStatus(orderKey.value(), status.name())
                .map(PaymentEntity::to);
    }
}
