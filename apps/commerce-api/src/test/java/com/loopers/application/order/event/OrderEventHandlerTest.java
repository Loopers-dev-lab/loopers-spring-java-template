package com.loopers.application.order.event;

import com.loopers.application.order.OrderFacade;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderDomainService;
import com.loopers.domain.order.Payment;
import com.loopers.domain.order.PaymentType;
import com.loopers.domain.order.event.OrderCompletedEvent;
import com.loopers.domain.order.event.PaymentCompletedEvent;
import com.loopers.domain.order.event.PaymentFailedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderEventHandlerTest {

    @Mock
    private OrderDomainService orderDomainService;

    @Mock
    private OrderFacade orderFacade;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderEventHandler orderEventHandler;

    @Test
    @DisplayName("결제 완료 이벤트를 받으면 주문을 확정하고 OrderCompletedEvent를 발행한다")
    void handlePaymentCompletedTest1() {
        // arrange
        PaymentCompletedEvent event = createPaymentCompletedEvent();
        Order mockOrder = mock(Order.class);
        when(orderDomainService.confirmOrder(event.getUserId(), event.getOrderId()))
                .thenReturn(mockOrder);

        // act
        orderEventHandler.handlePaymentCompleted(event);

        // assert
        verify(orderDomainService, times(1)).confirmOrder(event.getUserId(), event.getOrderId());
        verify(eventPublisher, times(1)).publishEvent(any(OrderCompletedEvent.class));
    }

    @Test
    @DisplayName("결제 완료 이벤트 처리 실패 시 예외를 전파한다")
    void handlePaymentCompletedTest2() {
        // arrange
        PaymentCompletedEvent event = createPaymentCompletedEvent();
        doThrow(new RuntimeException("DB error"))
                .when(orderDomainService).confirmOrder(event.getUserId(), event.getOrderId());

        // act & assert - 예외 전파 (Resilience4j @Retry는 통합테스트에서 동작)
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> orderEventHandler.handlePaymentCompleted(event));
        verify(orderDomainService, times(1)).confirmOrder(event.getUserId(), event.getOrderId());
    }

    @Test
    @DisplayName("결제 실패 이벤트를 받으면 주문 실패 처리를 한다")
    void handlePaymentFailedTest1() {
        // arrange
        PaymentFailedEvent event = createPaymentFailedEvent();

        // act
        orderEventHandler.handlePaymentFailed(event);

        // assert
        verify(orderFacade, times(1)).handlePaymentFailure(event.getUserId(), event.getOrderId());
    }

    @Test
    @DisplayName("결제 실패 이벤트 처리 실패 시 예외를 전파한다")
    void handlePaymentFailedTest2() {
        // arrange
        PaymentFailedEvent event = createPaymentFailedEvent();
        doThrow(new RuntimeException("DB error"))
                .when(orderFacade).handlePaymentFailure(event.getUserId(), event.getOrderId());

        // act & assert - 예외 전파 (Resilience4j @Retry는 통합테스트에서 동작)
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> orderEventHandler.handlePaymentFailed(event));
        verify(orderFacade, times(1)).handlePaymentFailure(event.getUserId(), event.getOrderId());
    }

    private PaymentCompletedEvent createPaymentCompletedEvent() {
        Payment payment = Payment.create(100L, "user123", 10000L, PaymentType.CARD_ONLY);
        ReflectionTestUtils.setField(payment, "id", 1L);
        payment.updatePgTransactionId("pg-tx-123");
        payment.markAsSuccess("pg-tx-123");
        return PaymentCompletedEvent.from(payment);
    }

    private PaymentFailedEvent createPaymentFailedEvent() {
        Payment payment = Payment.create(100L, "user123", 10000L, PaymentType.CARD_ONLY);
        ReflectionTestUtils.setField(payment, "id", 1L);
        payment.markAsFailed("결제 실패");
        return PaymentFailedEvent.from(payment);
    }
}
