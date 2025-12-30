package com.loopers.infrastructure.order;

import com.loopers.application.order.OrderPaymentProcessor;
import com.loopers.application.order.OrderPaymentSupport;
import com.loopers.domain.order.event.OrderEvent;
import com.loopers.infrastructure.payment.PgPaymentClient;
import com.loopers.infrastructure.payment.dto.PgPaymentV1Dto;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPaymentEventListener {

    private final PgPaymentClient pgPaymentClient;
    private final OrderPaymentProcessor orderPaymentProcessor;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OrderEvent.PaymentRequested event) {
        PgPaymentV1Dto.Request request = new PgPaymentV1Dto.Request(
                event.orderReference(),
                event.cardType(),
                event.cardNo(),
                event.totalAmount(),
                event.callbackUrl()
        );

        try {
            ApiResponse<PgPaymentV1Dto.Response> response = pgPaymentClient.requestPayment(event.userId(), request);
            PgPaymentV1Dto.Response pgData = OrderPaymentSupport.requirePgResponse(response);
            orderPaymentProcessor.handlePaymentResult(event.orderId(), pgData.status(), pgData.transactionKey(), pgData.reason());
        } catch (Exception exception) {
            log.error("결제 요청 처리 실패 orderId={}", event.orderId(), exception);
            orderPaymentProcessor.handlePaymentResult(event.orderId(), PgPaymentV1Dto.TransactionStatus.FAILED, null, exception.getMessage());
        }
    }

}
