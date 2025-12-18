package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.PaymentApproveInfo;
import com.loopers.domain.payment.PaymentApproveResponse;
import com.loopers.domain.payment.PaymentGateway;
import com.loopers.infrastructure.payment.feign.PgSimulatorFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class PaymentAdapter implements PaymentGateway {

    private final PgSimulatorFeignClient pgSimulatorFeignClient;

    @Override
    public PaymentApproveResponse approvePayment(final String userId, final PaymentApproveInfo request) {
        return pgSimulatorFeignClient.processPayment(userId, request);
    }
}
