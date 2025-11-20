package com.loopers.core.infra.database.mysql.payment.impl;

import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.repository.PaymentRepository;
import com.loopers.core.infra.database.mysql.payment.entity.PaymentEntity;
import com.loopers.core.infra.database.mysql.payment.repository.PaymentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {

    private final PaymentJpaRepository repository;

    @Override
    public Payment save(Payment payment) {
        return repository.save(PaymentEntity.from(payment)).to();
    }
}
