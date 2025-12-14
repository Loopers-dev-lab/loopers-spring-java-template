package com.loopers.core.domain.payment.repository;

import com.loopers.core.domain.payment.PgPayment;
import com.loopers.core.domain.payment.vo.PaymentId;
import com.loopers.core.domain.payment.vo.TransactionKey;

import java.util.Optional;

public interface PgPaymentRepository {

    Optional<PgPayment> findBy(PaymentId paymentId);

    PgPayment save(PgPayment pgPayment);

    PgPayment getByWithLock(TransactionKey transactionKey);
}
