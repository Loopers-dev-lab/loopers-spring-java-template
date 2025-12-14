package com.loopers.core.service.payment.component;

import com.loopers.core.domain.order.vo.OrderKey;
import com.loopers.core.domain.payment.repository.PaymentRepository;
import com.loopers.core.domain.payment.type.PaymentStatus;
import com.loopers.core.domain.payment.type.PgPaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentCallbackStrategySelector {

    private final PaymentRepository paymentRepository;
    private final SuccessfulPaymentStrategy successfulPaymentStrategy;
    private final FailedPaymentStrategy failedPaymentStrategy;
    private final DuplicateSuccessPaymentStrategy duplicateSuccessPaymentStrategy;

    public PaymentCallbackStrategy select(OrderKey orderKey, PgPaymentStatus status) {
        // 이미 성공한 결제가 있는 경우
        boolean hasSuccessfulPayment =
                paymentRepository.findBy(orderKey, PaymentStatus.SUCCESS).isPresent();

        if (hasSuccessfulPayment) {
            return duplicateSuccessPaymentStrategy;
        }

        // 결제가 실패한 경우
        if (status != PgPaymentStatus.SUCCESS) {
            return failedPaymentStrategy;
        }

        // 결제가 성공한 경우 (정상적인 흐름)
        return successfulPaymentStrategy;
    }
}
