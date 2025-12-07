package com.loopers.core.infra.database.mysql.payment.impl;

import com.loopers.core.domain.error.NotFoundException;
import com.loopers.core.domain.order.vo.OrderKey;
import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.repository.PaymentRepository;
import com.loopers.core.domain.payment.type.PaymentStatus;
import com.loopers.core.domain.payment.vo.TransactionKey;
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
    public Payment save(Payment payment) {
        return repository.save(PaymentEntity.from(payment)).to();
    }

    @Override
    public Payment getByWithLock(TransactionKey transactionKey) {
        return repository.findByTransactionKeyWithLock(Objects.requireNonNull(transactionKey.value()))
                .orElseThrow(() -> NotFoundException.withName("주문 정보"))
                .to();
    }

    @Override
    public Optional<Payment> findBy(OrderKey orderKey, PaymentStatus status) {
        return repository.findByOrderKeyAndStatus(orderKey.value(), status.name())
                .map(PaymentEntity::to);
    }

    @Override
    public Optional<Payment> findByWithLock(OrderKey orderKey, PaymentStatus status) {
        return repository.findByOrderKeyAndStatusWithLock(orderKey.value(), status.name())
                .map(PaymentEntity::to);
    }
}
