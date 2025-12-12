package com.loopers.domain.coupon.event;

import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.event.OrderEvents;
import com.loopers.domain.payment.event.PaymentEvents;
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

@DisplayName("CouponEventListener 단위 테스트 (Mock 사용)")
@ExtendWith(MockitoExtension.class)
class CouponEventListenerTest {

    @Mock
    private CouponService couponService;

    @Mock
    private OrderService orderService;

    @Mock
    private CouponEventPublisher couponEventPublisher;

    @InjectMocks
    private CouponEventListener couponEventListener;

    private StockEvents.Processed stockProcessedEvent;
    private PaymentEvents.ProcessingFailed paymentProcessingFailedEvent;

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
                .couponIds(List.of(10L, 20L))
                .build();

        OrderEvents.Created orderCreatedEvent = new OrderEvents.Created(
                1L, // userId
                100L, // orderId
                BigDecimal.valueOf(50000), // totalPrice
                request
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

    @DisplayName("handleStockProcessed 테스트")
    @Nested
    class HandleStockProcessedTest {

        @DisplayName("성공 케이스: 쿠폰이 없는 경우 CouponEvents.Processed 발행 (할인 금액 0)")
        @Test
        void handleStockProcessed_withNoCoupons_publishesCouponProcessedWithZeroDiscount() {
            // arrange
            OrderDto.CreateOrderRequest requestWithoutCoupons = OrderDto.CreateOrderRequest.builder()
                    .items(List.of(
                            OrderDto.OrderItemRequest.builder()
                                    .productId(1L)
                                    .quantity(2)
                                    .build()
                    ))
                    .couponIds(List.of())
                    .build();

            OrderEvents.Created orderCreatedEvent = new OrderEvents.Created(
                    1L, // userId
                    100L, // orderId
                    BigDecimal.valueOf(50000), // totalPrice
                    requestWithoutCoupons
            );

            StockEvents.Processed event = new StockEvents.Processed(
                    100L, // orderId
                    List.of(),
                    orderCreatedEvent
            );
            
            // Mock이 실제 이벤트를 발행하지 않도록 doNothing 설정
            doNothing().when(couponEventPublisher).publishCouponProcessed(any());

            // act
            couponEventListener.handleStockProcessed(event);

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

            // act
            couponEventListener.handleStockProcessed(stockProcessedEvent);

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

            // act
            couponEventListener.handleStockProcessed(stockProcessedEvent);

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

            // act
            couponEventListener.handlePaymentProcessingFailed(paymentProcessingFailedEvent);

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

            // act
            couponEventListener.handlePaymentProcessingFailed(paymentProcessingFailedEvent);

            // assert
            verify(couponService).rollbackCoupon(100L);
            verify(couponEventPublisher, never()).publishCouponCompensated(any());
        }
    }
}
