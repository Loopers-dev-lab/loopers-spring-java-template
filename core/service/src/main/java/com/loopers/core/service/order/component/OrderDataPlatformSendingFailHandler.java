package com.loopers.core.service.order.component;

import com.loopers.JacksonUtil;
import com.loopers.core.domain.error.HttpClientException;
import com.loopers.core.domain.event.EventOutbox;
import com.loopers.core.domain.event.repository.EventOutboxRepository;
import com.loopers.core.domain.event.type.AggregateType;
import com.loopers.core.domain.event.type.EventType;
import com.loopers.core.domain.event.vo.EventPayload;
import com.loopers.core.domain.order.vo.OrderId;
import com.loopers.core.service.order.event.OrderDataPlatformSendingFailEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderDataPlatformSendingFailHandler {

    private static final List<Class<? extends Exception>> RETRYABLE_EXCEPTIONS = List.of(
            HttpClientException.ServiceUnavailable.class,
            HttpClientException.InternalServerError.class,
            HttpClientException.GatewayTimeout.class,
            HttpClientException.TooManyRequests.class
    );

    private final EventOutboxRepository eventOutboxRepository;

    public void handle(OrderId orderId, Exception exception) {
        boolean retryable = RETRYABLE_EXCEPTIONS.stream()
                .anyMatch(exceptionClass -> exceptionClass.isInstance(exception));

        eventOutboxRepository.save(
                EventOutbox.create(
                        AggregateType.ORDER,
                        orderId.toAggregateId(),
                        EventType.PAYMENT_DATA_PLATFORM_SENDING_FAILED,
                        new EventPayload(
                                JacksonUtil.convertToString(
                                        OrderDataPlatformSendingFailEvent.create(
                                                orderId,
                                                retryable,
                                                exception.getMessage()
                                        )
                                )
                        )
                )
        );
    }
}
