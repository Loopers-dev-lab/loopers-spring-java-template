package com.loopers.application.payment.event;

import com.loopers.application.order.event.OrderCreated;
import com.loopers.application.payment.PaymentProcessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventHandler {

    private final PaymentProcessService paymentProcessService;

    /**
     * 외부 PG 결제 요청 시,
     * @param event
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    void handleExternalPgPayment(OrderCreated event) {
        paymentProcessService.processPg(event.userId(), event.orderId());
    }
}
