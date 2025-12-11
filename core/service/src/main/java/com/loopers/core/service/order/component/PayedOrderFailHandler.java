package com.loopers.core.service.order.component;

import com.loopers.JacksonUtil;
import com.loopers.core.domain.error.HttpClientException;
import com.loopers.core.domain.event.EventOutbox;
import com.loopers.core.domain.event.repository.EventOutboxRepository;
import com.loopers.core.domain.event.type.AggregateType;
import com.loopers.core.domain.event.type.EventType;
import com.loopers.core.domain.event.vo.EventPayload;
import com.loopers.core.domain.order.Order;
import com.loopers.core.domain.order.repository.OrderRepository;
import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.repository.PaymentRepository;
import com.loopers.core.service.order.event.PayedOrderFailEvent;
import com.loopers.core.service.payment.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PayedOrderFailHandler {

    private static final List<Class<? extends Exception>> RETRYABLE_EXCEPTIONS = List.of(
            HttpClientException.ServiceUnavailable.class,
            HttpClientException.InternalServerError.class,
            HttpClientException.GatewayTimeout.class,
            HttpClientException.TooManyRequests.class
    );

    private final EventOutboxRepository eventOutboxRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    public void handle(PaymentCompletedEvent event, Exception exception) {
        Payment payment = paymentRepository.getById(event.paymentId());
        Order order = orderRepository.getBy(payment.getOrderKey());
        boolean retryable = RETRYABLE_EXCEPTIONS.stream()
                .anyMatch(exceptionClass -> exceptionClass.isInstance(exception));

        eventOutboxRepository.save(
                EventOutbox.create(
                        AggregateType.ORDER,
                        order.getId().toAggregateId(),
                        EventType.PAYMENT_DATA_PLATFORM_SENDING_FAILED,
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
