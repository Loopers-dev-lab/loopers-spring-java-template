package com.loopers.core.infra.database.mysql.payment.impl;

import com.loopers.core.domain.error.NotFoundException;
import com.loopers.core.domain.payment.PgPayment;
import com.loopers.core.domain.payment.repository.PgPaymentRepository;
import com.loopers.core.domain.payment.vo.TransactionKey;
import com.loopers.core.infra.database.mysql.payment.entity.PgPaymentEntity;
import com.loopers.core.infra.database.mysql.payment.repository.PgPaymentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PgPaymentRepositoryImpl implements PgPaymentRepository {

    private final PgPaymentJpaRepository repository;

    @Override
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
