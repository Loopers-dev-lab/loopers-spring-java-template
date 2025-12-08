package com.loopers.core.domain.payment.repository;

import com.loopers.core.domain.order.vo.OrderKey;
import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.type.PaymentStatus;
import com.loopers.core.domain.payment.vo.PaymentId;

import java.util.Optional;

public interface PaymentRepository {

    Optional<Payment> findByWithLock(OrderKey orderKey, PaymentStatus status);

    Payment getById(PaymentId paymentId);

    Payment save(Payment payment);

    Optional<Payment> findBy(OrderKey orderKey, PaymentStatus status);
}
