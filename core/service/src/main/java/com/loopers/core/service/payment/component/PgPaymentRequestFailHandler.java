package com.loopers.core.service.payment.component;

import com.loopers.JacksonUtil;
import com.loopers.core.domain.event.EventOutbox;
import com.loopers.core.domain.event.repository.EventOutboxRepository;
import com.loopers.core.domain.event.type.AggregateType;
import com.loopers.core.domain.event.type.EventType;
import com.loopers.core.domain.event.vo.EventPayload;
import com.loopers.core.domain.payment.vo.PaymentId;
import com.loopers.core.domain.payment.vo.PgPaymentRequestFailEvent;
import com.loopers.core.service.config.RetryableExceptionsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PgPaymentRequestFailHandler {

    private final EventOutboxRepository eventOutboxRepository;
    private final RetryableExceptionsProperties retryableExceptionsProperties;

    public void handle(PaymentId paymentId, Exception exception) {
        boolean retryable = retryableExceptionsProperties.isRetryable(exception);

        eventOutboxRepository.save(
                EventOutbox.create(
                        AggregateType.PAYMENT,
                        paymentId.toAggregateId(),
                        EventType.PG_PAYMENT_FAILED,
                        new EventPayload(
                                JacksonUtil.convertToString(
                                        PgPaymentRequestFailEvent.create(
                                                paymentId,
                                                retryable,
                                                exception.getMessage()
                                        )
                                )
                        )
                )
        );
    }
}
