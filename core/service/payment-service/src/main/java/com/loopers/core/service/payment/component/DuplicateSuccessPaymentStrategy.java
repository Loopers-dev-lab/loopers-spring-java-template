package com.loopers.core.service.payment.component;

import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.vo.FailedReason;
import org.springframework.stereotype.Component;

@Component
public class DuplicateSuccessPaymentStrategy implements PaymentCallbackStrategy {

    @Override
    public Payment pay(Payment payment, FailedReason failedReason) {
        return payment.fail(
                new FailedReason("이미 결제에 성공한 이력이 있는 주문입니다.")
        );
    }
}
