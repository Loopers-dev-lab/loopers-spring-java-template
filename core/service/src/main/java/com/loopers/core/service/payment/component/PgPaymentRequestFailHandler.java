package com.loopers.core.service.payment.component;

import com.loopers.JacksonUtil;
import com.loopers.core.domain.error.HttpClientException;
import com.loopers.core.domain.event.EventOutbox;
import com.loopers.core.domain.event.repository.EventOutboxRepository;
import com.loopers.core.domain.event.type.AggregateType;
import com.loopers.core.domain.event.type.EventType;
import com.loopers.core.domain.event.vo.EventPayload;
import com.loopers.core.domain.payment.vo.PaymentId;
import com.loopers.core.service.payment.event.PgPaymentRequestFailEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PgPaymentRequestFailHandler {

    private static final List<Class<? extends Exception>> RETRYABLE_EXCEPTIONS = List.of(
            HttpClientException.ServiceUnavailable.class,
            HttpClientException.InternalServerError.class,
            HttpClientException.GatewayTimeout.class,
            HttpClientException.TooManyRequests.class
    );

    private final EventOutboxRepository eventOutboxRepository;

    public void handle(PaymentId paymentId, Exception exception) {
        boolean retryable = RETRYABLE_EXCEPTIONS.stream()
                .anyMatch(exceptionClass -> exceptionClass.isInstance(exception));

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
