package com.loopers.domain.payment.event;

import com.loopers.domain.coupon.event.CouponEvents;
import com.loopers.domain.order.event.OrderEvents;
import com.loopers.domain.payment.*;
import com.loopers.domain.payment.strategy.PaymentStrategy;
import com.loopers.domain.payment.strategy.PaymentStrategyFactory;
import com.loopers.domain.stock.event.StockEvents;
import com.loopers.interfaces.api.order.OrderDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@DisplayName("PaymentEventListener 단위 테스트 (Mock 사용)")
@ExtendWith(MockitoExtension.class)
class PaymentEventListenerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private PaymentEventPublisher paymentEventPublisher;

    @Mock
    private PaymentStrategyFactory paymentStrategyFactory;

    @Mock
    private PaymentStrategy paymentStrategy;

    @InjectMocks
    private PaymentEventListener paymentEventListener;

    private CouponEvents.Processed couponProcessedEvent;
    private PaymentEvents.CallbackReceived callbackReceivedEvent;

    @BeforeEach
    void setUp() {
        // 테스트용 OrderEvents.Created 생성
        OrderDto.CreateOrderRequest request = OrderDto.CreateOrderRequest.builder()
                .items(List.of(
                        OrderDto.OrderItemRequest.builder()
                                .productId(1L)
                                .quantity(2)
                                .build()
                ))
                .couponIds(List.of())
                .paymentMethod(PaymentDto.PaymentMethod.CARD)
                .build();

        OrderEvents.Created orderCreatedEvent = new OrderEvents.Created(
                1L, // userId
                100L, // orderId
                BigDecimal.valueOf(50000), // totalPrice
                request
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

    @DisplayName("handleCouponProcessed 테스트")
    @Nested
    class HandleCouponProcessedTest {

        @DisplayName("성공 케이스: 결제 금액이 0원 이하인 경우 PaymentEvents.Processed 발행 (결제 처리 생략)")
        @Test
        void handleCouponProcessed_withZeroAmount_publishesPaymentProcessedWithoutProcessing() {
            // arrange
            // totalPrice와 동일한 할인 금액으로 설정하여 결제 금액이 0원이 되도록 함
            BigDecimal totalPrice = couponProcessedEvent.originalEvent().originalEvent().totalPrice();
            CouponEvents.Processed eventWithFullDiscount = new CouponEvents.Processed(
                    100L, // orderId
                    1L, // userId
                    totalPrice, // totalDiscountAmount (totalPrice와 동일하여 결제 금액 0원)
                    couponProcessedEvent.originalEvent()
            );

            // act
            paymentEventListener.handleCouponProcessed(eventWithFullDiscount);

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

            // act
            paymentEventListener.handleCouponProcessed(couponProcessedEvent);

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

            // act
            paymentEventListener.handleCouponProcessed(couponProcessedEvent);

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

            // act
            paymentEventListener.handlePaymentCallbackReceived(successEvent);

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

            // act
            paymentEventListener.handlePaymentCallbackReceived(failedEvent);

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
