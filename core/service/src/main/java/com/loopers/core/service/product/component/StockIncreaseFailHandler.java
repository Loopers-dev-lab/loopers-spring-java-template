package com.loopers.core.service.product.component;

import com.loopers.JacksonUtil;
import com.loopers.core.domain.event.EventOutbox;
import com.loopers.core.domain.event.repository.EventOutboxRepository;
import com.loopers.core.domain.event.type.AggregateType;
import com.loopers.core.domain.event.type.EventType;
import com.loopers.core.domain.event.vo.EventId;
import com.loopers.core.domain.event.vo.EventPayload;
import com.loopers.core.domain.payment.vo.PaymentFailedEvent;
import com.loopers.core.domain.payment.vo.PaymentId;
import com.loopers.core.domain.product.vo.StockIncreaseFailEvent;
import com.loopers.core.service.config.RetryableExceptionsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockIncreaseFailHandler {

    private final EventOutboxRepository eventOutboxRepository;
    private final RetryableExceptionsProperties retryableExceptionsProperties;

    public void handle(PaymentFailedEvent event, Exception exception) {
        PaymentId paymentId = event.paymentId();
        boolean retryable = retryableExceptionsProperties.isRetryable(exception);

        eventOutboxRepository.save(
                EventOutbox.create(
                        EventId.generate(),
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
