package com.loopers.application.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentStatus;
import com.loopers.infrastructure.payment.client.PgClient;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.payment.PaymentV1Dto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentFacade {
    private final PgClient pgClient;
    private final PaymentRepository paymentRepository;

    @CircuitBreaker(name = "pgCircuit", fallbackMethod = "fallback")
    public PaymentV1Dto.TransactionResponse requestPayment(String userId, PaymentV1Dto.PgPaymentRequest pgPaymentRequest) {
        ApiResponse<PaymentV1Dto.TransactionResponse> result = pgClient.requestPayment(userId, pgPaymentRequest);

        PaymentV1Dto.TransactionResponse response = result.data();

        PaymentV1Dto.PaymentRequest paymentRequest = new PaymentV1Dto.PaymentRequest(
                userId,
                pgPaymentRequest.orderId(),
                response.transactionKey()
        );

        Payment payment = Payment.create(
                paymentRequest.userId(),
                paymentRequest.orderNo(),
                paymentRequest.transactionKey(),
                PaymentStatus.PENDING
        );
        paymentRepository.save(payment);

        return response;
    }

    public PaymentV1Dto.TransactionResponse fallback(String userId, PaymentV1Dto.PgPaymentRequest request, Throwable throwable) {
        return new PaymentV1Dto.TransactionResponse(
                null,
                PaymentV1Dto.TransactionStatusResponse.FAILED,
                "현재 결제 서비스가 불안정합니다. 잠시 후 다시 시도해주세요."
        );
    }
}
