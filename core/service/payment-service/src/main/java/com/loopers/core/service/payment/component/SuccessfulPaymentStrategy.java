package com.loopers.core.service.payment.component;

import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.vo.FailedReason;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component
@RequiredArgsConstructor
public class SuccessfulPaymentStrategy implements PaymentCallbackStrategy {

    @Override
    @Transactional
    public Payment pay(Payment payment, FailedReason failedReason) {
        return payment.success();
    }
}
