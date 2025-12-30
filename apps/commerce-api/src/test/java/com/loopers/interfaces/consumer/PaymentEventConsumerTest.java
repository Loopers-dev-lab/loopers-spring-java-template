package com.loopers.interfaces.consumer;

import com.loopers.domain.coupon.event.CouponEvents;
import com.loopers.domain.event.InboxEventService;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.event.OrderEvents;
import com.loopers.domain.payment.*;
import com.loopers.domain.payment.event.PaymentEventPublisher;
import com.loopers.domain.payment.event.PaymentEventHandler;
import com.loopers.domain.payment.event.PaymentEvents;
import com.loopers.domain.payment.strategy.PaymentStrategy;
import com.loopers.domain.payment.strategy.PaymentStrategyFactory;
import com.loopers.domain.stock.event.StockEvents;
import com.loopers.event.consumer.KafkaMessageProcessor;
import com.loopers.infrastructure.payment.event.PaymentInboxEventRepository;
import com.loopers.shared.event.DomainEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("PaymentEventConsumer 단위 테스트 (Mock 사용)")
@ExtendWith(MockitoExtension.class)
class PaymentEventConsumerTest {

    @Mock
    private KafkaMessageProcessor messageProcessor;

    @Mock
    private PaymentService paymentService;

    @Mock
    private PaymentEventPublisher paymentEventPublisher;

    @Mock
    private PaymentStrategyFactory paymentStrategyFactory;

    @Mock
    private PaymentStrategy paymentStrategy;

    @Mock
    private OrderService orderService;

    @Mock
    private PaymentInboxEventRepository paymentInboxEventRepository;

    @Mock
    private InboxEventService inboxEventService;

    @Mock
    private Acknowledgment acknowledgment;

    private PaymentEventHandler paymentEventHandler;
    private KafkaPaymentEventConsumer paymentConsumer;

    private CouponEvents.Processed couponProcessedEvent;
    private PaymentEvents.CallbackReceived callbackReceivedEvent;

    @BeforeEach
    void setUp() {
        // PaymentEventHandler 실제 인스턴스 생성 (의존성은 Mock으로 주입)
        paymentEventHandler = new PaymentEventHandler(
                paymentService,
                paymentEventPublisher,
                paymentStrategyFactory,
                orderService,
                paymentInboxEventRepository,
                inboxEventService
        );

        // InboxEventService Mock 설정 - 중복 체크 실패(false)로 설정하여 실제 로직 실행되도록
        when(inboxEventService.checkAndSave(any(), any(), anyString(), any())).thenReturn(false);

        // KafkaMessageProcessor Mock 설정 - 비즈니스 로직 실행하도록
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            ConsumerRecord<String, DomainEvent> record = (ConsumerRecord<String, DomainEvent>) invocation.getArgument(0);
            @SuppressWarnings("unchecked")
            KafkaMessageProcessor.BusinessLogic<DomainEvent> businessLogic = (KafkaMessageProcessor.BusinessLogic<DomainEvent>) invocation.getArgument(3);
            businessLogic.execute(record.value());
            return null;
        }).when(messageProcessor).execute(any(), any(), anyString(), any());

        // KafkaPaymentEventConsumer 수동 생성 (PaymentEventHandler 실제 인스턴스 사용)
        paymentConsumer = new KafkaPaymentEventConsumer(messageProcessor, paymentEventHandler);

        // 테스트용 OrderEvents.Created 생성
        List<OrderEvents.OrderItemInfo> items = List.of(
                new OrderEvents.OrderItemInfo(1L, "Test Product", BigDecimal.valueOf(25000), 2)
        );
        List<Long> couponIds = List.of();

        OrderEvents.Created orderCreatedEvent = new OrderEvents.Created(
                100L, // orderId
                1L, // userId
                BigDecimal.valueOf(50000), // totalAmount
                items,
                couponIds,
                PaymentDto.PaymentMethod.CARD
        );

        // 테스트용 StockEvents.Processed 생성
        StockEvents.Processed stockProcessedEvent = new StockEvents.Processed(
                100L, // orderId
                List.of(),
                orderCreatedEvent
        );

        couponProcessedEvent = new CouponEvents.Processed(
                100L, // orderId
                1L, // userId
                BigDecimal.valueOf(5000), // totalDiscountAmount
                stockProcessedEvent
        );

        // 테스트용 PaymentEvents.CallbackReceived 생성
        callbackReceivedEvent = new PaymentEvents.CallbackReceived(
                "TEST_TRANSACTION_KEY",
                100L, // orderId
                PaymentDto.PaymentStatus.SUCCESS,
                null // reason
        );
    }

    // ConsumerRecord 헬퍼 메서드
    private <T> ConsumerRecord<String, T> createConsumerRecord(String topic, T value) {
        return new ConsumerRecord<>(topic, 0, 0L, "key", value);
    }

    @DisplayName("handleCouponProcessed 테스트")
    @Nested
    class HandleCouponProcessedTest {

        @DisplayName("성공 케이스: 결제 금액이 0원 이하인 경우 PaymentEvents.Processed 발행 (결제 처리 생략)")
        @Test
        void handleCouponProcessed_withZeroAmount_publishesPaymentProcessedWithoutProcessing() {
            // arrange
            // totalAmount와 동일한 할인 금액으로 설정하여 결제 금액이 0원이 되도록 함
            BigDecimal totalAmount = couponProcessedEvent.originalEvent().originalEvent().totalAmount();
            CouponEvents.Processed eventWithFullDiscount = new CouponEvents.Processed(
                    100L, // orderId
                    1L, // userId
                    totalAmount, // totalDiscountAmount (totalAmount와 동일하여 결제 금액 0원)
                    couponProcessedEvent.originalEvent()
            );

            ConsumerRecord<String, CouponEvents.Processed> record = 
                    createConsumerRecord("coupon.v1", eventWithFullDiscount);

            // act
            paymentConsumer.handleCouponProcessed(record, acknowledgment);

            // assert
            verify(paymentStrategyFactory, never()).getStrategy(any());
            verify(paymentStrategy, never()).processPayment(anyLong(), anyLong(), any());
            verify(paymentService, never()).saveCommercePayment(any());
            verify(paymentEventPublisher).publishPaymentProcessed(argThat(processed ->
                    processed.orderId().equals(100L) &&
                            processed.userId().equals(1L) &&
                            processed.finalAmount().compareTo(BigDecimal.ZERO) <= 0
            ));
        }

        @DisplayName("성공 케이스: 결제 처리 성공 시 PaymentEvents.Processed 발행")
        @Test
        void handleCouponProcessed_withValidPayment_publishesPaymentProcessed() {
            // arrange
            when(paymentStrategyFactory.getStrategy(any())).thenReturn(paymentStrategy);
            when(paymentStrategy.getPaymentMethod()).thenReturn(PaymentDto.PaymentMethod.CARD);
            when(paymentStrategy.processPayment(anyLong(), anyLong(), any()))
                    .thenReturn(new PaymentStrategy.PaymentResult(
                            true, // success
                            "TEST_TRANSACTION_KEY",
                            PaymentDto.PaymentStatus.SUCCESS,
                            null // reason
                    ));
            doReturn(mock(CommercePayment.class)).when(paymentService).saveCommercePayment(any(CommercePayment.class));

            ConsumerRecord<String, CouponEvents.Processed> record = 
                    createConsumerRecord("coupon.v1", couponProcessedEvent);

            // act
            paymentConsumer.handleCouponProcessed(record, acknowledgment);

            // assert
            verify(paymentStrategyFactory).getStrategy(any());
            verify(paymentStrategy).processPayment(eq(100L), eq(1L), any(BigDecimal.class));
            verify(paymentService).saveCommercePayment(any(CommercePayment.class));
            verify(paymentEventPublisher).publishPaymentProcessed(argThat(processed ->
                    processed.orderId().equals(100L) &&
                            processed.userId().equals(1L)
            ));
            verify(paymentEventPublisher, never()).publishPaymentProcessingFailed(any());
        }

        @DisplayName("실패 케이스: 결제 처리 실패 시 PaymentEvents.ProcessingFailed 발행")
        @Test
        void handleCouponProcessed_withPaymentFailure_publishesPaymentProcessingFailed() {
            // arrange
            when(paymentStrategyFactory.getStrategy(any())).thenReturn(paymentStrategy);
            when(paymentStrategy.getPaymentMethod()).thenReturn(PaymentDto.PaymentMethod.CARD);
            when(paymentStrategy.processPayment(anyLong(), anyLong(), any()))
                    .thenReturn(new PaymentStrategy.PaymentResult(
                            false, // success
                            "TEST_TRANSACTION_KEY",
                            PaymentDto.PaymentStatus.FAILED,
                            "결제 요청에 실패했습니다"
                    ));

            ConsumerRecord<String, CouponEvents.Processed> record = 
                    createConsumerRecord("coupon.v1", couponProcessedEvent);

            // act
            paymentConsumer.handleCouponProcessed(record, acknowledgment);

            // assert
            verify(paymentStrategyFactory).getStrategy(any());
            verify(paymentStrategy).processPayment(eq(100L), eq(1L), any(BigDecimal.class));
            verify(paymentService, never()).saveCommercePayment(any());
            verify(paymentEventPublisher, never()).publishPaymentProcessed(any());
            verify(paymentEventPublisher).publishPaymentProcessingFailed(argThat(failed ->
                    failed.orderId().equals(100L) &&
                            failed.reason().contains("결제 요청에 실패했습니다")
            ));
        }

        @DisplayName("실패 케이스: PaymentStrategy 예외 발생 시 PaymentEvents.ProcessingFailed 발행")
        @Test
        void handleCouponProcessed_withPaymentStrategyException_publishesPaymentProcessingFailed() {
            // arrange
            when(paymentStrategyFactory.getStrategy(any())).thenReturn(paymentStrategy);
            when(paymentStrategy.getPaymentMethod()).thenReturn(PaymentDto.PaymentMethod.CARD);
            when(paymentStrategy.processPayment(anyLong(), anyLong(), any()))
                    .thenThrow(new RuntimeException("PG 서버 연결 실패"));

            // Mock이 실제 이벤트를 발행하지 않도록 doNothing 설정
            doNothing().when(paymentEventPublisher).publishPaymentProcessingFailed(any());

            ConsumerRecord<String, CouponEvents.Processed> record = 
                    createConsumerRecord("coupon.v1", couponProcessedEvent);

            // act
            paymentConsumer.handleCouponProcessed(record, acknowledgment);

            // assert
            verify(paymentStrategyFactory).getStrategy(any());
            verify(paymentStrategy).processPayment(eq(100L), eq(1L), any(BigDecimal.class));
            verify(paymentService, never()).saveCommercePayment(any());
            verify(paymentEventPublisher, never()).publishPaymentProcessed(any());
            verify(paymentEventPublisher).publishPaymentProcessingFailed(argThat(failed ->
                    failed.orderId().equals(100L) &&
                            failed.reason().contains("PG 서버 연결 실패")
            ));
        }
    }

    @DisplayName("handlePaymentCallbackReceived 테스트")
    @Nested
    class HandlePaymentCallbackReceivedTest {

        @DisplayName("성공 케이스: PG 콜백 성공 시 PaymentEvents.Processed 발행")
        @Test
        void handlePaymentCallbackReceived_withSuccessStatus_publishesPaymentProcessed() {
            // arrange
            PaymentEvents.CallbackReceived successEvent = new PaymentEvents.CallbackReceived(
                    "TEST_TRANSACTION_KEY",
                    100L, // orderId
                    PaymentDto.PaymentStatus.SUCCESS,
                    null // reason
            );
            doNothing().when(paymentService).saveSuccessPayment(anyString());
            
            // Order와 CommercePayment Mock 설정
            Order mockOrder = mock(Order.class);
            when(mockOrder.getUserId()).thenReturn(1L);
            when(orderService.findOrderById(100L)).thenReturn(mockOrder);
            
            CommercePayment mockCommercePayment = mock(CommercePayment.class);
            when(mockCommercePayment.getAmount()).thenReturn(BigDecimal.valueOf(50000));
            when(paymentService.findByTransactionKey("TEST_TRANSACTION_KEY")).thenReturn(mockCommercePayment);

            ConsumerRecord<String, PaymentEvents.CallbackReceived> record = 
                    createConsumerRecord("payment.v1", successEvent);

            // act
            paymentConsumer.handlePaymentCallbackReceived(record, acknowledgment);

            // assert
            verify(paymentService).saveSuccessPayment("TEST_TRANSACTION_KEY");
            verify(paymentEventPublisher).publishPaymentProcessed(any(PaymentEvents.Processed.class));
            verify(paymentService, never()).saveFailedPayment(anyString(), anyString());
            verify(paymentEventPublisher, never()).publishPaymentProcessingFailed(any());
        }

        @DisplayName("실패 케이스: PG 콜백 실패 시 PaymentEvents.ProcessingFailed 발행")
        @Test
        void handlePaymentCallbackReceived_withFailedStatus_publishesPaymentProcessingFailed() {
            // arrange
            PaymentEvents.CallbackReceived failedEvent = new PaymentEvents.CallbackReceived(
                    "TEST_TRANSACTION_KEY",
                    100L, // orderId
                    PaymentDto.PaymentStatus.FAILED,
                    "결제 요청에 실패했습니다"
            );
            doNothing().when(paymentService).saveFailedPayment(anyString(), anyString());

            ConsumerRecord<String, PaymentEvents.CallbackReceived> record = 
                    createConsumerRecord("payment.v1", failedEvent);

            // act
            paymentConsumer.handlePaymentCallbackReceived(record, acknowledgment);

            // assert
            verify(paymentService).saveFailedPayment("TEST_TRANSACTION_KEY", "결제 요청에 실패했습니다");
            verify(paymentEventPublisher).publishPaymentProcessingFailed(argThat(failed ->
                    failed.orderId().equals(100L) &&
                            failed.reason().equals("결제 요청에 실패했습니다")
            ));
            verify(paymentService, never()).saveSuccessPayment(anyString());
            verify(paymentEventPublisher, never()).publishPaymentProcessed(any());
        }
    }
}

