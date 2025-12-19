package com.loopers.interfaces.listener;

import com.loopers.application.payment.PaymentEvent;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Component
public class PaymentStatusUpdateListener {

    private final PaymentService paymentService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handle(PaymentEvent.PaymentStatusUpdateRequest event) {
        log.info("PaymentStatusUpdateRequest 이벤트 수신 - Payment 상태 업데이트: orderKey={}, status={}, transactionKey={}",
                event.orderKey(), event.status(), event.transactionKey());

        Payment payment = paymentService.getPaymentByOrderKey(event.orderKey());
        payment.updateStatus(event.status());

        if (event.transactionKey() != null) {
            payment.updateTransactionKey(event.transactionKey());
        }
    }
}

