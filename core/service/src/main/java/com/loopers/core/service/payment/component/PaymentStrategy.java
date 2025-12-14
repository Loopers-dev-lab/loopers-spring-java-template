package com.loopers.core.service.payment.component;

import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.type.PaymentType;

public interface PaymentStrategy {

    PaymentType getPaymentType();

    void pay(Payment payment);
}
