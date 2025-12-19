package com.loopers.interfaces.listener;

import com.loopers.application.payment.PaymentEvent;
import com.loopers.domain.dataplatform.DataPlatformClient;
import com.loopers.domain.dataplatform.PaymentData;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.ZonedDateTime;

@Slf4j
@RequiredArgsConstructor
@Component
public class PaymentDataPlatformListener {

    private final PaymentService paymentService;
    private final DataPlatformClient dataPlatformClient;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentCompleted(PaymentEvent.PaymentCompleted event) {
        log.info("PaymentCompleted 이벤트 수신 - 데이터 플랫폼 전송: orderKey={}, transactionKey={}",
                event.orderKey(), event.transactionKey());
        Payment payment = paymentService.getPaymentByOrderKey(event.orderKey());

        PaymentData paymentData = new PaymentData(
                payment.getOrderKey(),
                payment.getTransactionKey(),
                payment.getAmount(),
                payment.getStatus().name(),
                null,
                payment.getCreatedAt() != null
                        ? payment.getCreatedAt()
                        : ZonedDateTime.now()
        );

        dataPlatformClient.sendPaymentData(paymentData);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentFailed(PaymentEvent.PaymentFailed event) {
        log.info("PaymentFailed 이벤트 수신 - 데이터 플랫폼 전송: orderKey={}, reason={}",
                event.orderKey(), event.reason());

        Payment payment = paymentService.getPaymentByOrderKey(event.orderKey());

        PaymentData paymentData = new PaymentData(
                payment.getOrderKey(),
                payment.getTransactionKey(),
                payment.getAmount(),
                payment.getStatus().name(),
                event.reason(),
                payment.getCreatedAt() != null
                        ? payment.getCreatedAt()
                        : ZonedDateTime.now()
        );

        dataPlatformClient.sendPaymentData(paymentData);
    }
}

