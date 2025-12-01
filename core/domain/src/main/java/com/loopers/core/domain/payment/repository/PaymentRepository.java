package com.loopers.core.domain.payment.repository;

import com.loopers.core.domain.order.vo.OrderKey;
import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.type.PaymentStatus;
import com.loopers.core.domain.payment.vo.TransactionKey;

import java.util.Optional;

public interface PaymentRepository {

    Payment save(Payment payment);

    Payment getByWithLock(TransactionKey transactionKey);

    Optional<Payment> findBy(OrderKey orderKey, PaymentStatus status);

    Optional<Payment> findByWithLock(OrderKey orderKey, PaymentStatus status);
}
