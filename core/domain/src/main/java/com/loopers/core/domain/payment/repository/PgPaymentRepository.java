package com.loopers.core.domain.payment.repository;

import com.loopers.core.domain.payment.PgPayment;
import com.loopers.core.domain.payment.vo.TransactionKey;

public interface PgPaymentRepository {

    PgPayment save(PgPayment pgPayment);

    PgPayment getByWithLock(TransactionKey transactionKey);
}
