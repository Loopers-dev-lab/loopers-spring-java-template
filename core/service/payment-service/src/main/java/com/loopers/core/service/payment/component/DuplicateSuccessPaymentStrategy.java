package com.loopers.core.service.payment.component;

import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.PgPayment;
import com.loopers.core.domain.payment.repository.PaymentRepository;
import com.loopers.core.domain.payment.vo.FailedReason;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DuplicateSuccessPaymentStrategy implements PaymentCallbackStrategy {

    private final PaymentRepository paymentRepository;

    @Override
    public void pay(PgPayment pgPayment, FailedReason failedReason) {
        Payment payment = paymentRepository.getById(pgPayment.getPaymentId());
        paymentRepository.save(payment.failed(new FailedReason("이미 결제에 성공한 이력이 있는 주문입니다.")));
    }
}
