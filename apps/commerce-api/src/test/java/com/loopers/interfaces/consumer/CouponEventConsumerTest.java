package com.loopers.interfaces.consumer;

import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.coupon.event.CouponEventPublisher;
import com.loopers.domain.coupon.event.CouponEvents;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.event.OrderEvents;
import com.loopers.domain.payment.PaymentDto;
import com.loopers.domain.payment.event.PaymentEvents;
import com.loopers.domain.stock.event.StockEvents;
import com.loopers.event.consumer.KafkaMessageProcessor;
import com.loopers.shared.event.DomainEvent;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("CouponEventListener 단위 테스트 (Mock 사용)")
@ExtendWith(MockitoExtension.class)
class CouponEventConsumerTest {

    @Mock
    private KafkaMessageProcessor messageProcessor;

    @Mock
    private CouponService couponService;

    @Mock
    private OrderService orderService;

    @Mock
    private CouponEventPublisher couponEventPublisher;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter counter;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private KafkaCouponEventConsumer couponConsumer;

    private StockEvents.Processed stockProcessedEvent;
    private PaymentEvents.ProcessingFailed paymentProcessingFailedEvent;

    @BeforeEach
    void setUp() {
        // KafkaMessageProcessor Mock 설정 - 비즈니스 로직 실행하도록
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            ConsumerRecord<String, DomainEvent> record = (ConsumerRecord<String, DomainEvent>) invocation.getArgument(0);
            @SuppressWarnings("unchecked")
            KafkaMessageProcessor.BusinessLogic<DomainEvent> businessLogic = (KafkaMessageProcessor.BusinessLogic<DomainEvent>) invocation.getArgument(3);
            businessLogic.execute(record.value());
            return null;
        }).when(messageProcessor).execute(any(), any(), anyString(), any());

        // MeterRegistry Mock 설정
        when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);

        // 테스트용 OrderEvents.Created 생성
        List<OrderEvents.OrderItemInfo> items = List.of(
                new OrderEvents.OrderItemInfo(1L, "Test Product", BigDecimal.valueOf(25000), 2)
        );
        List<Long> couponIds = List.of(10L, 20L);

        OrderEvents.Created orderCreatedEvent = new OrderEvents.Created(
                100L, // orderId
                1L, // userId
                BigDecimal.valueOf(50000), // totalPrice
                items,
                couponIds,
                PaymentDto.PaymentMethod.CARD
        );

        // 테스트용 StockEvents.Processed 생성
        stockProcessedEvent = new StockEvents.Processed(
                100L, // orderId
                List.of(
                        new StockEvents.OrderItemInfo(1L, 2)
                ),
                orderCreatedEvent
        );

        // 테스트용 CouponEvents.Processed 생성 (PaymentEvents.ProcessingFailed용)
        CouponEvents.Processed couponProcessedEvent = new CouponEvents.Processed(
                100L, // orderId
                1L, // userId
                BigDecimal.valueOf(5000), // totalDiscountAmount
                stockProcessedEvent // originalEvent
        );

        // 테스트용 PaymentEvents.ProcessingFailed 생성
        paymentProcessingFailedEvent = new PaymentEvents.ProcessingFailed(
                100L, // orderId
                couponProcessedEvent, // originalEvent
                "결제 처리 실패"
        );
    }

    // ConsumerRecord 헬퍼 메서드
    private <T> ConsumerRecord<String, T> createConsumerRecord(String topic, T value) {
        return new ConsumerRecord<>(topic, 0, 0L, "key", value);
    }

    @DisplayName("handleStockProcessed 테스트")
    @Nested
    class HandleStockProcessedTest {

        @DisplayName("성공 케이스: 쿠폰이 없는 경우 CouponEvents.Processed 발행 (할인 금액 0)")
        @Test
        void handleStockProcessed_withNoCoupons_publishesCouponProcessedWithZeroDiscount() {
            // arrange
            List<OrderEvents.OrderItemInfo> items = List.of(
                    new OrderEvents.OrderItemInfo(1L, "Test Product", BigDecimal.valueOf(25000), 2)
            );
            List<Long> emptyCouponIds = List.of();

            OrderEvents.Created orderCreatedEvent = new OrderEvents.Created(
                    100L, // orderId
                    1L, // userId
                    BigDecimal.valueOf(50000), // totalPrice
                    items,
                    emptyCouponIds,
                    PaymentDto.PaymentMethod.CARD
            );

            StockEvents.Processed event = new StockEvents.Processed(
                    100L, // orderId
                    List.of(),
                    orderCreatedEvent
            );
            
            // Mock이 실제 이벤트를 발행하지 않도록 doNothing 설정
            doNothing().when(couponEventPublisher).publishCouponProcessed(any());

            ConsumerRecord<String, StockEvents.Processed> record = 
                    createConsumerRecord("stock.deducted.v1", event);

            // act
            couponConsumer.handleStockProcessed(record, acknowledgment);

            // assert
            verify(couponService, never()).useCoupon(anyLong(), anyLong(), any(), anyLong());
            verify(couponEventPublisher).publishCouponProcessed(argThat(couponProcessed ->
                    couponProcessed.orderId().equals(100L) &&
                    couponProcessed.userId().equals(1L) &&
                    couponProcessed.totalDiscountAmount().compareTo(BigDecimal.ZERO) == 0
            ));
            verify(couponEventPublisher, never()).publishCouponProcessingFailed(any());
        }

        @DisplayName("성공 케이스: 쿠폰 사용 성공 시 CouponEvents.Processed 발행")
        @Test
        void handleStockProcessed_withValidCoupons_publishesCouponProcessed() {
            // arrange
            when(couponService.useCoupon(anyLong(), anyLong(), any(), anyLong()))
                    .thenReturn(BigDecimal.valueOf(5000))
                    .thenReturn(BigDecimal.valueOf(3000));
            
            // Mock이 실제 이벤트를 발행하지 않도록 doNothing 설정
            doNothing().when(couponEventPublisher).publishCouponProcessed(any());

            ConsumerRecord<String, StockEvents.Processed> record = 
                    createConsumerRecord("stock.deducted.v1", stockProcessedEvent);

            // act
            couponConsumer.handleStockProcessed(record, acknowledgment);

            // assert
            verify(couponService, times(2)).useCoupon(anyLong(), anyLong(), any(), anyLong());
            verify(couponService).useCoupon(100L, 1L, BigDecimal.valueOf(50000), 10L);
            verify(couponService).useCoupon(100L, 1L, BigDecimal.valueOf(50000), 20L);
            verify(orderService).applyDiscount(eq(100L), eq(BigDecimal.valueOf(8000)));
            verify(couponEventPublisher).publishCouponProcessed(argThat(couponProcessed ->
                    couponProcessed.orderId().equals(100L) &&
                    couponProcessed.userId().equals(1L) &&
                    couponProcessed.totalDiscountAmount().compareTo(BigDecimal.valueOf(8000)) == 0
            ));
            verify(couponEventPublisher, never()).publishCouponProcessingFailed(any());
        }

        @DisplayName("실패 케이스: 쿠폰 사용 실패 시 CouponEvents.ProcessingFailed 발행")
        @Test
        void handleStockProcessed_withCouponServiceException_publishesCouponProcessingFailed() {
            // arrange
            when(couponService.useCoupon(anyLong(), anyLong(), any(), anyLong()))
                    .thenThrow(new RuntimeException("쿠폰 사용 실패"));
            
            // Mock이 실제 이벤트를 발행하지 않도록 doNothing 설정
            doNothing().when(couponEventPublisher).publishCouponProcessingFailed(any());

            ConsumerRecord<String, StockEvents.Processed> record = 
                    createConsumerRecord("stock.deducted.v1", stockProcessedEvent);

            // act
            couponConsumer.handleStockProcessed(record, acknowledgment);

            // assert
            verify(couponService, atLeastOnce()).useCoupon(anyLong(), anyLong(), any(), anyLong());
            verify(couponEventPublisher, never()).publishCouponProcessed(any());
            verify(couponEventPublisher).publishCouponProcessingFailed(argThat(failed ->
                    failed.orderId().equals(100L) &&
                    failed.reason().contains("쿠폰 사용 실패")
            ));
        }
    }

    @DisplayName("handlePaymentProcessingFailed 테스트")
    @Nested
    class HandlePaymentProcessingFailedTest {

        @DisplayName("성공 케이스: 쿠폰 원복 성공 시 CouponEvents.Compensated 발행")
        @Test
        void handlePaymentProcessingFailed_withValidEvent_rollsBackCoupon() {
            // arrange
            doNothing().when(couponService).rollbackCoupon(anyLong());
            // Mock이 실제 이벤트를 발행하지 않도록 doNothing 설정
            doNothing().when(couponEventPublisher).publishCouponCompensated(any());

            ConsumerRecord<String, PaymentEvents.ProcessingFailed> record = 
                    createConsumerRecord("payment.failed.v1", paymentProcessingFailedEvent);

            // act
            couponConsumer.handlePaymentProcessingFailed(record, acknowledgment);

            // assert
            verify(couponService).rollbackCoupon(100L);
            verify(couponEventPublisher).publishCouponCompensated(argThat(compensated ->
                    compensated.orderId().equals(100L)
            ));
        }

        @DisplayName("실패 케이스: 쿠폰 원복 실패 시 예외 처리 (보상 트랜잭션 실패)")
        @Test
        void handlePaymentProcessingFailed_withRollbackException_handlesException() {
            // arrange
            doThrow(new RuntimeException("쿠폰 원복 실패")).when(couponService).rollbackCoupon(anyLong());

            ConsumerRecord<String, PaymentEvents.ProcessingFailed> record = 
                    createConsumerRecord("payment.failed.v1", paymentProcessingFailedEvent);

            // act
            couponConsumer.handlePaymentProcessingFailed(record, acknowledgment);

            // assert
            verify(couponService).rollbackCoupon(100L);
            verify(couponEventPublisher, never()).publishCouponCompensated(any());
        }
    }
}

