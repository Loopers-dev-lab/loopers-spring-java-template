package com.loopers.core.infra.database.mysql.payment.impl;

import com.loopers.core.domain.error.NotFoundException;
import com.loopers.core.domain.payment.PgPayment;
import com.loopers.core.domain.payment.repository.PgPaymentRepository;
import com.loopers.core.domain.payment.vo.PaymentId;
import com.loopers.core.domain.payment.vo.TransactionKey;
import com.loopers.core.infra.database.mysql.payment.entity.PgPaymentEntity;
import com.loopers.core.infra.database.mysql.payment.repository.PgPaymentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PgPaymentRepositoryImpl implements PgPaymentRepository {

    private final PgPaymentJpaRepository repository;

    @Override
    public Optional<PgPayment> findBy(PaymentId paymentId) {
        return repository.findByPaymentId(Long.parseLong(Objects.requireNonNull(paymentId.value())))
                .map(PgPaymentEntity::to);
    }

    @Override
    @Transactional
    public PgPayment save(PgPayment pgPayment) {
        return repository.save(PgPaymentEntity.from(pgPayment)).to();
    }

    @Override
    public PgPayment getByWithLock(TransactionKey transactionKey) {
        return repository.findByTransactionKey(transactionKey.value())
                .orElseThrow(() -> NotFoundException.withName("PG 결제"))
                .to();
    }
}
