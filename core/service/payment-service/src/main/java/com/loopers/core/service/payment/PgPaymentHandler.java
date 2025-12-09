package com.loopers.core.service.payment;

import com.loopers.JacksonUtil;
import com.loopers.core.domain.event.EventOutbox;
import com.loopers.core.domain.event.repository.EventOutboxRepository;
import com.loopers.core.domain.event.type.AggregateType;
import com.loopers.core.domain.event.type.EventType;
import com.loopers.core.domain.event.vo.EventPayload;
import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.PgClient;
import com.loopers.core.domain.payment.PgPayment;
import com.loopers.core.domain.payment.repository.PaymentRepository;
import com.loopers.core.domain.payment.repository.PgPaymentRepository;
import com.loopers.core.domain.payment.type.PaymentType;
import com.loopers.core.domain.payment.vo.FailedReason;
import com.loopers.core.domain.payment.vo.PaymentId;
import com.loopers.core.service.payment.event.PaymentCreatedEvent;
import com.loopers.core.service.payment.event.PgPaymentRequestFailEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

@Slf4j
@Component
@RequiredArgsConstructor
public class PgPaymentHandler {

    private final PgClient pgClient;
    private final PgPaymentRepository pgPaymentRepository;
    private final PaymentRepository paymentRepository;
    private final EventOutboxRepository eventOutboxRepository;

    @Value("${pg.callback.url}")
    String callbackUrl;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handle(PaymentCreatedEvent event) {
        if (event.paymentType() == PaymentType.POINT) {
            return;
        }

        PaymentId paymentId = event.paymentId();
        Payment payment = paymentRepository.getById(paymentId);

        try {
            PgPayment pgPayment = pgClient.pay(payment, callbackUrl);
            pgPaymentRepository.findBy(paymentId)
                    .map(exist -> pgPaymentRepository.save(exist.merge(pgPayment)))
                    .orElseGet(() -> pgPaymentRepository.save(pgPayment));
        } catch (Exception e) {
            log.error("PG 결제 요청 처리중 에러가 발생했습니다.", e);
            eventOutboxRepository.save(
                    EventOutbox.create(
                            AggregateType.PAYMENT,
                            paymentId.toAggregateId(),
                            EventType.PG_PAYMENT_FAILED,
                            new EventPayload(
                                    JacksonUtil.convertToString(
                                            new PgPaymentRequestFailEvent(paymentId, e.getMessage())
                                    )
                            )
                    )
            );
            paymentRepository.save(payment.failed(new FailedReason(e.getMessage())));
        }
    }
}
