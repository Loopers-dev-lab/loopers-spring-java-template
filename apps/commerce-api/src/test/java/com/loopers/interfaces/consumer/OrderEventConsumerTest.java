package com.loopers.interfaces.consumer;

import com.loopers.domain.event.InboxEventService;
import com.loopers.domain.coupon.event.CouponEvents;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.order.event.OrderEventPublisher;
import com.loopers.domain.payment.event.PaymentEvents;
import com.loopers.domain.stock.event.StockEvents;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("OrderEventListener 단위 테스트 (Mock 사용)")
@ExtendWith(MockitoExtension.class)
class OrderEventConsumerTest {

    @Mock
    private InboxEventService inboxEventService;

    @Mock
    private OrderService orderService;

    @Mock
    private OrderEventPublisher orderEventPublisher;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter counter;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private KafkaOrderEventConsumer orderConsumer;

    private PaymentEvents.Processed paymentProcessedEvent;
    private StockEvents.ProcessingFailed stockProcessingFailedEvent;
    private CouponEvents.ProcessingFailed couponProcessingFailedEvent;
    private PaymentEvents.ProcessingFailed paymentProcessingFailedEvent;

    @BeforeEach
    void setUp() {
        // InboxEventService Mock 설정 - Runnable action을 실행하도록
        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(2);
            action.run();
            return null;
        }).when(inboxEventService).process(anyString(), any(LocalDateTime.class), any(Runnable.class));

        // MeterRegistry Mock 설정
        when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);

        // 테스트용 PaymentEvents.Processed 생성
        paymentProcessedEvent = new PaymentEvents.Processed(
                100L, // orderId
                1L, // userId
                BigDecimal.valueOf(45000), // finalAmount
                null // originalEvent (PG 콜백 경로)
        );

        // 테스트용 StockEvents.ProcessingFailed 생성
        stockProcessingFailedEvent = new StockEvents.ProcessingFailed(
                100L, // orderId
                null, // orderItems
                "재고 부족"
        );

        // 테스트용 CouponEvents.ProcessingFailed 생성
        couponProcessingFailedEvent = new CouponEvents.ProcessingFailed(
                100L, // orderId
                null, // originalEvent
                "쿠폰 사용 실패"
        );

        // 테스트용 PaymentEvents.ProcessingFailed 생성
        paymentProcessingFailedEvent = new PaymentEvents.ProcessingFailed(
                100L, // orderId
                null, // originalEvent
                "결제 처리 실패"
        );
    }

    // ConsumerRecord 헬퍼 메서드
    private <T> ConsumerRecord<String, T> createConsumerRecord(String topic, T value) {
        return new ConsumerRecord<>(topic, 0, 0L, "key", value);
    }

    @DisplayName("handlePaymentProcessed 테스트")
    @Nested
    class HandlePaymentProcessedTest {

        @DisplayName("성공 케이스: 주문 상태 CONFIRMED로 변경 및 OrderEvents.Confirmed 발행")
        @Test
        void handlePaymentProcessed_withValidEvent_confirmsOrderAndPublishesOrderConfirmed() {
            // arrange
            Order mockOrder = mock(Order.class);
            when(mockOrder.getId()).thenReturn(100L);
            when(mockOrder.getUserId()).thenReturn(1L);
            when(mockOrder.getOrderStatus()).thenReturn(OrderStatus.CONFIRMED);
            when(orderService.saveSuccessOrder(anyLong())).thenReturn(mockOrder);

            ConsumerRecord<String, PaymentEvents.Processed> record = 
                    createConsumerRecord("payment.completed.v1", paymentProcessedEvent);

            // act
            orderConsumer.handlePaymentProcessed(record, acknowledgment);

            // assert
            verify(orderService).saveSuccessOrder(100L);
            verify(orderEventPublisher).publishOrderConfirmed(argThat(confirmed ->
                    confirmed.orderId().equals(100L) &&
                    confirmed.userId().equals(1L) &&
                    confirmed.orderStatus().equals("CONFIRMED")
            ));
        }
    }

    @DisplayName("handleStockProcessingFailed 테스트")
    @Nested
    class HandleStockProcessingFailedTest {

        @DisplayName("성공 케이스: 주문 상태 FAILED로 변경")
        @Test
        void handleStockProcessingFailed_withValidEvent_savesFailedOrder() {
            // arrange
            // saveFailedOrder의 반환값은 사용되지 않으므로 간단한 mock 반환
            Order mockOrder = mock(Order.class);
            when(orderService.saveFailedOrder(anyLong(), anyString())).thenReturn(mockOrder);

            ConsumerRecord<String, StockEvents.ProcessingFailed> record = 
                    createConsumerRecord("stock.deduction-failed.v1", stockProcessingFailedEvent);

            // act
            orderConsumer.handleStockProcessingFailed(record, acknowledgment);

            // assert
            verify(orderService).saveFailedOrder(100L, "재고 부족");
            verify(orderEventPublisher, never()).publishOrderConfirmed(any());
        }
    }

    @DisplayName("handleCouponProcessingFailed 테스트")
    @Nested
    class HandleCouponProcessingFailedTest {

        @DisplayName("성공 케이스: 주문 상태 FAILED로 변경")
        @Test
        void handleCouponProcessingFailed_withValidEvent_savesFailedOrder() {
            // arrange
            // saveFailedOrder의 반환값은 사용되지 않으므로 간단한 mock 반환
            Order mockOrder = mock(Order.class);
            when(orderService.saveFailedOrder(anyLong(), anyString())).thenReturn(mockOrder);

            ConsumerRecord<String, CouponEvents.ProcessingFailed> record = 
                    createConsumerRecord("coupon.apply-failed.v1", couponProcessingFailedEvent);

            // act
            orderConsumer.handleCouponProcessingFailed(record, acknowledgment);

            // assert
            verify(orderService).saveFailedOrder(100L, "쿠폰 사용 실패");
            verify(orderEventPublisher, never()).publishOrderConfirmed(any());
        }
    }

    @DisplayName("handlePaymentProcessingFailed 테스트")
    @Nested
    class HandlePaymentProcessingFailedTest {

        @DisplayName("성공 케이스: 주문 상태 FAILED로 변경")
        @Test
        void handlePaymentProcessingFailed_withValidEvent_savesFailedOrder() {
            // arrange
            // saveFailedOrder의 반환값은 사용되지 않으므로 간단한 mock 반환
            Order mockOrder = mock(Order.class);
            when(orderService.saveFailedOrder(anyLong(), anyString())).thenReturn(mockOrder);

            ConsumerRecord<String, PaymentEvents.ProcessingFailed> record = 
                    createConsumerRecord("payment.failed.v1", paymentProcessingFailedEvent);

            // act
            orderConsumer.handlePaymentProcessingFailed(record, acknowledgment);

            // assert
            verify(orderService).saveFailedOrder(100L, "결제 처리 실패");
            verify(orderEventPublisher, never()).publishOrderConfirmed(any());
        }
    }
}

