package com.loopers.core.service.payment.component;

import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.PgPayment;
import com.loopers.core.domain.payment.repository.PaymentRepository;
import com.loopers.core.domain.payment.vo.FailedReason;
import com.loopers.core.domain.payment.vo.PaymentFailedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class FailedPaymentStrategy implements PaymentCallbackStrategy {

    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public Payment pay(PgPayment pgPayment, FailedReason failedReason) {
        Payment payment = paymentRepository.getById(pgPayment.getPaymentId());
        eventPublisher.publishEvent(new PaymentFailedEvent(payment.getId(), failedReason));

        return paymentRepository.save(payment.failed(failedReason));
    }
}
