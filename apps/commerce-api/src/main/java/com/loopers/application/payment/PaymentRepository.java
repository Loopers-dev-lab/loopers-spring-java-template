package com.loopers.application.payment;

import com.loopers.domain.payment.Payment;

public interface PaymentRepository {
    Payment save(Payment payment);
}
