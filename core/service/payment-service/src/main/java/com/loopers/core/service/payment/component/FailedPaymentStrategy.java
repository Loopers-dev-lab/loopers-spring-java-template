package com.loopers.core.service.payment.component;

import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.vo.FailedReason;
import org.springframework.stereotype.Component;

@Component
public class FailedPaymentStrategy implements PaymentCallbackStrategy {

    @Override
    public Payment pay(Payment payment, FailedReason failedReason) {
        return payment.fail(failedReason);
    }
}
