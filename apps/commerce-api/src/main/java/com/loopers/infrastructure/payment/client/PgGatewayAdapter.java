package com.loopers.infrastructure.payment.client;

import org.springframework.stereotype.Component;

import com.loopers.domain.payment.PgGateway;
import com.loopers.infrastructure.payment.client.dto.PgPaymentRequest;
import com.loopers.infrastructure.payment.client.dto.PgPaymentResponse;

import lombok.RequiredArgsConstructor;

/**
 * PgGateway 구현체 (Adapter)
 * <p>
 * Domain Layer의 PgGateway 인터페이스를 구현하여
 * Infrastructure Layer의 PgClient를 연결합니다.
 *
 * @author hyunjikoh
 * @since 2025. 12. 5.
 */
@Component
@RequiredArgsConstructor
public class PgGatewayAdapter implements PgGateway {
    private final PgClient pgClient;

    @Override
    public PgPaymentResponse requestPayment(String username, PgPaymentRequest request) {
        return pgClient.requestPayment(username, request);
    }

    @Override
    public PgPaymentResponse getPayment(String username, String transactionKey) {
        return pgClient.getPayment(username, transactionKey);
    }
}
