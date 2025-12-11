package com.loopers.core.service.product.component;

import com.loopers.JacksonUtil;
import com.loopers.core.domain.error.HttpClientException;
import com.loopers.core.domain.event.EventOutbox;
import com.loopers.core.domain.event.repository.EventOutboxRepository;
import com.loopers.core.domain.event.type.AggregateType;
import com.loopers.core.domain.event.type.EventType;
import com.loopers.core.domain.event.vo.EventPayload;
import com.loopers.core.domain.payment.vo.PaymentId;
import com.loopers.core.service.payment.event.PaymentFailedEvent;
import com.loopers.core.service.product.event.StockIncreaseFailEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StockIncreaseFailHandler {

    private static final List<Class<? extends Exception>> RETRYABLE_EXCEPTIONS = List.of(
            HttpClientException.ServiceUnavailable.class,
            HttpClientException.InternalServerError.class,
            HttpClientException.GatewayTimeout.class,
            HttpClientException.TooManyRequests.class
    );

    private final EventOutboxRepository eventOutboxRepository;
    
    public void handle(PaymentFailedEvent event, Exception exception) {
        PaymentId paymentId = event.paymentId();
        boolean retryable = RETRYABLE_EXCEPTIONS.stream()
                .anyMatch(exceptionClass -> exceptionClass.isInstance(exception));

        eventOutboxRepository.save(
                EventOutbox.create(
                        AggregateType.PRODUCT,
                        paymentId.toAggregateId(),
                        EventType.STOCK_INCREASE_FAIL,
                        new EventPayload(
                                JacksonUtil.convertToString(
                                        StockIncreaseFailEvent.create(paymentId, exception.getMessage(), retryable)
                                )
                        )
                )
        );
    }
}
