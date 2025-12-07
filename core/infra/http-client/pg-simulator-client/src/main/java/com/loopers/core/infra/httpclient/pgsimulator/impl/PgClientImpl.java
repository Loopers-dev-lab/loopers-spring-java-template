package com.loopers.core.infra.httpclient.pgsimulator.impl;

import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.PgClient;
import com.loopers.core.domain.payment.PgPayment;
import com.loopers.core.domain.payment.type.PaymentStatus;
import com.loopers.core.domain.payment.vo.FailedReason;
import com.loopers.core.domain.payment.vo.TransactionKey;
import com.loopers.core.infra.httpclient.pgsimulator.PgSimulatorClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static com.loopers.core.infra.httpclient.pgsimulator.dto.PgSimulatorV1Dto.PgSimulatorPaymentRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class PgClientImpl implements PgClient {

    private final PgSimulatorClient pgSimulatorClient;

    @Override
    @Retry(name = "pgRetry")
    @CircuitBreaker(name = "pgCircuitBreaker", fallbackMethod = "fallback")
    public PgPayment pay(Payment payment, String callbackUrl) {
        return Objects.requireNonNull(pgSimulatorClient.pay(payment.getUserId().value(), PgSimulatorPaymentRequest.from(payment, callbackUrl)).getBody()).to();
    }

    public PgPayment fallback(Payment payment, String callbackUrl, Throwable throwable) {
        return new PgPayment(TransactionKey.empty(), PaymentStatus.NO_RESPONSE, new FailedReason(throwable.getMessage()));
    }
}
