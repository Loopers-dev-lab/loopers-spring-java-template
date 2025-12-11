package com.loopers.core.service.payment.component;

import com.loopers.JacksonUtil;
import com.loopers.core.domain.error.HttpClientException;
import com.loopers.core.domain.event.EventOutbox;
import com.loopers.core.domain.event.repository.EventOutboxRepository;
import com.loopers.core.domain.event.type.AggregateType;
import com.loopers.core.domain.event.type.EventType;
import com.loopers.core.domain.event.vo.EventPayload;
import com.loopers.core.service.payment.event.PaymentCompletedEvent;
import com.loopers.core.service.payment.event.PaymentDataFlatformSendingFailEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PaymentDataPlatformSendingFailHandler {

    private static final List<Class<? extends Exception>> RETRYABLE_EXCEPTIONS = List.of(
            HttpClientException.ServiceUnavailable.class,
            HttpClientException.InternalServerError.class,
            HttpClientException.GatewayTimeout.class,
            HttpClientException.TooManyRequests.class
    );

    private final EventOutboxRepository eventOutboxRepository;

    @Transactional
    public void handle(PaymentCompletedEvent event, Exception exception) {
        boolean retryable = RETRYABLE_EXCEPTIONS.stream()
                .anyMatch(exceptionClass -> exceptionClass.isInstance(exception));

        eventOutboxRepository.save(
                EventOutbox.create(
                        AggregateType.PAYMENT,
                        event.paymentId().toAggregateId(),
                        EventType.PAYMENT_DATA_PLATFORM_SENDING_FAILED,
                        new EventPayload(
                                JacksonUtil.convertToString(
                                        PaymentDataFlatformSendingFailEvent.create(
                                                event.paymentId(),
                                                retryable,
                                                exception.getMessage()
                                        )
                                )
                        )
                )
        );
    }
}
