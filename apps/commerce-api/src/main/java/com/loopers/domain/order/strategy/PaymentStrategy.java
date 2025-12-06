package com.loopers.domain.order.strategy;

import com.loopers.domain.order.PaymentType;

public interface PaymentStrategy {

    boolean supports(PaymentType paymentType);

    void executePayment(PaymentContext context);
}