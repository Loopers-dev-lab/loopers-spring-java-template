package com.loopers.domain.order.strategy;

import com.loopers.domain.order.PaymentDomainService;
import com.loopers.domain.order.PaymentType;
import com.loopers.infrastructure.pg.PgClient;
import com.loopers.infrastructure.pg.dto.PgPaymentRequest;
import com.loopers.infrastructure.pg.dto.PgPaymentResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CardPaymentStrategy implements PaymentStrategy {

    private final PgClient pgClient;
    private final PaymentDomainService paymentDomainService;

    @Value("${pg.callback-base-url}")
    private String callbackBaseUrl;

    @Override
    public boolean supports(PaymentType paymentType) {
        return paymentType == PaymentType.CARD_ONLY;
    }

    @Override
    @Retry(name = "pgRetry")
    @CircuitBreaker(name = "pgCircuit", fallbackMethod = "fallbackRequestPayment")
    public void executePayment(PaymentContext context) {
        PgPaymentRequest request = PgPaymentRequest.builder()
                .orderId(String.format("%06d", context.orderId()))
                .cardType(context.cardType())
                .cardNo(context.cardNo())
                .amount(context.cardAmount())
                .callbackUrl(callbackBaseUrl + "/api/v1/payments/callback")
                .build();

        PgPaymentResponse response = pgClient.requestPayment(context.userId(), request);
        paymentDomainService.updatePgTransactionId(context.paymentId(), response.getTransactionKey());
    }

    public void fallbackRequestPayment(PaymentContext context, Throwable throwable) {
        log.warn("PG request timeout/failure fallback. orderId: {}, paymentId: {}, reason: {}",
                context.orderId(), context.paymentId(), throwable.getClass().getSimpleName());
    }
}