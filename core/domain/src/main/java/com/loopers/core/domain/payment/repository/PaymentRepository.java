package com.loopers.core.domain.payment.repository;

import com.loopers.core.domain.payment.Payment;

public interface PaymentRepository {

    Payment save(Payment payment);
}
