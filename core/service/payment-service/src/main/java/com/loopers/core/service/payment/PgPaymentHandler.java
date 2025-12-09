package com.loopers.core.service.payment;

import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.PgClient;
import com.loopers.core.domain.payment.PgPayment;
import com.loopers.core.domain.payment.repository.PaymentRepository;
import com.loopers.core.domain.payment.repository.PgPaymentRepository;
import com.loopers.core.domain.payment.vo.FailedReason;
import com.loopers.core.service.payment.event.PgPaymentEvent;
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

    @Value("${pg.callback.url}")
    String callbackUrl;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handle(PgPaymentEvent pgPaymentEvent) {
        Payment payment = pgPaymentEvent.payment();

        try {
            PgPayment pgPayment = pgClient.pay(payment, callbackUrl);
            pgPaymentRepository.findBy(payment.getId())
                    .map(exist -> pgPaymentRepository.save(exist.merge(pgPayment)))
                    .orElseGet(() -> pgPaymentRepository.save(pgPayment));
        } catch (Exception e) {
            //TODO Outbox로 실패 저장
            log.error("PG 결제 요청 처리중 에러가 발생했습니다.", e);
            paymentRepository.save(payment.failed(new FailedReason(e.getMessage())));
        }
    }
}
