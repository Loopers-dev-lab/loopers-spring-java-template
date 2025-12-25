package com.loopers.core.service.order.component;

import com.loopers.JacksonUtil;
import com.loopers.core.domain.event.EventOutbox;
import com.loopers.core.domain.event.repository.EventOutboxRepository;
import com.loopers.core.domain.event.type.AggregateType;
import com.loopers.core.domain.event.type.EventType;
import com.loopers.core.domain.event.vo.EventId;
import com.loopers.core.domain.event.vo.EventPayload;
import com.loopers.core.domain.order.Order;
import com.loopers.core.domain.order.repository.OrderRepository;
import com.loopers.core.domain.order.vo.PayedOrderFailEvent;
import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.repository.PaymentRepository;
import com.loopers.core.domain.payment.vo.PaymentCompletedEvent;
import com.loopers.core.service.config.RetryableExceptionsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PayedOrderFailHandler {

    private final EventOutboxRepository eventOutboxRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final RetryableExceptionsProperties retryableExceptionsProperties;

    public void handle(PaymentCompletedEvent event, Exception exception) {
        Payment payment = paymentRepository.getById(event.paymentId());
        Order order = orderRepository.getBy(payment.getOrderKey());
        boolean retryable = retryableExceptionsProperties.isRetryable(exception);

        eventOutboxRepository.save(
                EventOutbox.create(
                        EventId.generate(),
                        AggregateType.ORDER,
                        order.getId().toAggregateId(),
                        EventType.PAYED_ORDER_STATUS_CHANGED_FAIL,
                        new EventPayload(
                                JacksonUtil.convertToString(
                                        PayedOrderFailEvent.create(
                                                payment.getId(),
                                                order.getId(),
                                                retryable,
                                                exception.getMessage()
                                        )
                                )
                        )
                )
        );
    }
}
