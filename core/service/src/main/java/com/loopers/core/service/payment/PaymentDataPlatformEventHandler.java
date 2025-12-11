package com.loopers.core.service.payment;

import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.PaymentDataPlatformClient;
import com.loopers.core.domain.payment.repository.PaymentRepository;
import com.loopers.core.domain.payment.vo.PaymentCompletedEvent;
import com.loopers.core.service.payment.component.PaymentDataPlatformSendingFailHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentDataPlatformEventHandler {

    private final PaymentDataPlatformSendingFailHandler paymentDataPlatformSendingFailHandler;
    private final PaymentRepository paymentRepository;
    private final PaymentDataPlatformClient dataPlatformClient;


    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = AFTER_COMMIT)
    void handle(PaymentCompletedEvent event) {
        try {
            Payment payment = paymentRepository.getById(event.paymentId());
            dataPlatformClient.send(payment);
        } catch (Exception exception) {
            log.error("결제 데이터플랫폼 전송 중 오류가 발생했습니다.", exception);
            paymentDataPlatformSendingFailHandler.handle(event, exception);
        }
    }
}
