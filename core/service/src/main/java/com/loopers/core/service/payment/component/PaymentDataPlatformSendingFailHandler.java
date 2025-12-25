package com.loopers.core.service.payment.component;

import com.loopers.JacksonUtil;
import com.loopers.core.domain.event.EventOutbox;
import com.loopers.core.domain.event.repository.EventOutboxRepository;
import com.loopers.core.domain.event.type.AggregateType;
import com.loopers.core.domain.event.type.EventType;
import com.loopers.core.domain.event.vo.EventId;
import com.loopers.core.domain.event.vo.EventPayload;
import com.loopers.core.domain.payment.vo.PaymentCompletedEvent;
import com.loopers.core.domain.payment.vo.PaymentDataPlatformSendingFailEvent;
import com.loopers.core.service.config.RetryableExceptionsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PaymentDataPlatformSendingFailHandler {

    private final EventOutboxRepository eventOutboxRepository;
    private final RetryableExceptionsProperties retryableExceptionsProperties;

    @Transactional
    public void handle(PaymentCompletedEvent event, Exception exception) {
        boolean retryable = retryableExceptionsProperties.isRetryable(exception);

        eventOutboxRepository.save(
                EventOutbox.create(
                        EventId.generate(),
                        AggregateType.PAYMENT,
                        event.paymentId().toAggregateId(),
                        EventType.PAYMENT_DATA_PLATFORM_SENDING_FAILED,
                        new EventPayload(
                                JacksonUtil.convertToString(
                                        PaymentDataPlatformSendingFailEvent.create(
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
