package com.loopers.core.service.order.component;

import com.loopers.JacksonUtil;
import com.loopers.core.domain.event.EventOutbox;
import com.loopers.core.domain.event.repository.EventOutboxRepository;
import com.loopers.core.domain.event.type.AggregateType;
import com.loopers.core.domain.event.type.EventType;
import com.loopers.core.domain.event.vo.EventPayload;
import com.loopers.core.domain.order.vo.OrderCompletedEvent;
import com.loopers.core.domain.order.vo.OrderDataPlatformSendingFailEvent;
import com.loopers.core.domain.order.vo.OrderId;
import com.loopers.core.service.config.RetryableExceptionsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OrderDataPlatformSendingFailHandler {

    private final EventOutboxRepository eventOutboxRepository;
    private final RetryableExceptionsProperties retryableExceptionsProperties;

    @Transactional
    public void handle(OrderCompletedEvent event, Exception exception) {
        OrderId orderId = event.orderId();
        boolean retryable = retryableExceptionsProperties.isRetryable(exception);

        eventOutboxRepository.save(
                EventOutbox.create(
                        AggregateType.ORDER,
                        orderId.toAggregateId(),
                        EventType.ORDER_DATA_PLATFORM_SENDING_FAILED,
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
