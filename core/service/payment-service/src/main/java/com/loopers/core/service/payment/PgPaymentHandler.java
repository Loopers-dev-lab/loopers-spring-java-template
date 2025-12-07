package com.loopers.core.service.payment;

import com.loopers.core.domain.payment.PgClient;
import com.loopers.core.service.payment.event.PgPaymentEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

@Component
@RequiredArgsConstructor
public class PgPaymentHandler {

    private final PgClient pgClient;

    @Value("${pg.callback.url}")
    String callbackUrl;

    @Async
    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handle(PgPaymentEvent pgPaymentEvent) {

    }
}
