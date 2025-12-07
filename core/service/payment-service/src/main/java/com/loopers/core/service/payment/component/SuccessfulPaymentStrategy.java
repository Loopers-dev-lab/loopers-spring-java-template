package com.loopers.core.service.payment.component;

import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.PgPayment;
import com.loopers.core.domain.payment.repository.PaymentRepository;
import com.loopers.core.domain.payment.vo.FailedReason;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component
@RequiredArgsConstructor
public class SuccessfulPaymentStrategy implements PaymentCallbackStrategy {

    private final PaymentRepository paymentRepository;

    @Override
    @Transactional
    public void pay(PgPayment pgPayment, FailedReason failedReason) {
        Payment payment = paymentRepository.getById(pgPayment.getPaymentId());
        paymentRepository.save(payment.success());
    }
}
