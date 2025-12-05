package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.PaymentApproveInfo;
import com.loopers.domain.payment.PaymentApproveResponse;
import com.loopers.domain.payment.PaymentGateway;
import com.loopers.infrastructure.payment.feign.PgSimulatorFeignClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class PaymentAdapter implements PaymentGateway {

    private final PgSimulatorFeignClient pgSimulatorFeignClient;

    @Override
    public PaymentApproveResponse approvePayment(final String mallName, final PaymentApproveInfo request) {
        return pgSimulatorFeignClient.processPayment(mallName, request);
    }
}
