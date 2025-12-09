package com.loopers.core.service.payment;

import com.loopers.JacksonUtil;
import com.loopers.core.domain.event.EventOutbox;
import com.loopers.core.domain.event.repository.EventOutboxRepository;
import com.loopers.core.domain.event.type.AggregateType;
import com.loopers.core.domain.event.type.EventType;
import com.loopers.core.domain.event.vo.EventPayload;
import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.PaymentDataPlatformClient;
import com.loopers.core.domain.payment.repository.PaymentRepository;
import com.loopers.core.service.payment.event.PaymentDataFlatformSendingFailEvent;
import com.loopers.core.service.payment.event.PgPaymentCompletedEvent;
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

    private final EventOutboxRepository eventOutboxRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentDataPlatformClient dataPlatformClient;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = AFTER_COMMIT)
    void handle(PgPaymentCompletedEvent event) {
        try {
            Payment payment = paymentRepository.getById(event.paymentId());
            dataPlatformClient.send(payment);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            eventOutboxRepository.save(
                    EventOutbox.create(
                            AggregateType.PAYMENT,
                            event.paymentId().toAggregateId(),
                            EventType.PAYMENT_DATA_PLATFORM_SENDING_FAILED,
                            new EventPayload(
                                    JacksonUtil.convertToString(
                                            new PaymentDataFlatformSendingFailEvent(event.paymentId(), e.getMessage())
                                    )
                            )
                    )
            );
        }
    }
}
