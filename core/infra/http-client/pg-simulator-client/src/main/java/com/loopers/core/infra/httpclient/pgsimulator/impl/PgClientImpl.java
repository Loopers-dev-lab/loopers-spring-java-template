package com.loopers.core.infra.httpclient.pgsimulator.impl;

import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.PgClient;
import com.loopers.core.domain.payment.PgPayment;
import com.loopers.core.infra.httpclient.pgsimulator.PgSimulatorClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static com.loopers.core.infra.httpclient.pgsimulator.dto.PgSimulatorV1Dto.PgSimulatorPaymentRequest;

@Component
@RequiredArgsConstructor
public class PgClientImpl implements PgClient {

    private final PgSimulatorClient pgSimulatorClient;

    @Override
    public PgPayment pay(Payment payment, String callbackUrl) {
        return Objects.requireNonNull(
                pgSimulatorClient.pay(
                        payment.getUserId().value(),
                        PgSimulatorPaymentRequest.from(payment, callbackUrl)
                ).getBody()).to();
    }
}
