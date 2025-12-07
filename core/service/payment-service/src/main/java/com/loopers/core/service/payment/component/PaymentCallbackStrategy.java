package com.loopers.core.service.payment.component;

import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.vo.FailedReason;

public interface PaymentCallbackStrategy {

    Payment pay(Payment payment, FailedReason failedReason);
}
