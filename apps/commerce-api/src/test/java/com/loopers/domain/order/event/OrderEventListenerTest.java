package com.loopers.domain.order.event;

import com.loopers.domain.coupon.event.CouponEvents;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.payment.event.PaymentEvents;
import com.loopers.domain.stock.event.StockEvents;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("OrderEventListener 단위 테스트 (Mock 사용)")
@ExtendWith(MockitoExtension.class)
class OrderEventListenerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private OrderEventPublisher orderEventPublisher;

    @InjectMocks
    private OrderEventListener orderEventListener;

    private PaymentEvents.Processed paymentProcessedEvent;
    private StockEvents.ProcessingFailed stockProcessingFailedEvent;
    private CouponEvents.ProcessingFailed couponProcessingFailedEvent;
    private PaymentEvents.ProcessingFailed paymentProcessingFailedEvent;

    @BeforeEach
    void setUp() {
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

            // act
            orderEventListener.handlePaymentProcessed(paymentProcessedEvent);

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

            // act
            orderEventListener.handleStockProcessingFailed(stockProcessingFailedEvent);

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

            // act
            orderEventListener.handleCouponProcessingFailed(couponProcessingFailedEvent);

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

            // act
            orderEventListener.handlePaymentProcessingFailed(paymentProcessingFailedEvent);

            // assert
            verify(orderService).saveFailedOrder(100L, "결제 처리 실패");
            verify(orderEventPublisher, never()).publishOrderConfirmed(any());
        }
    }
}
