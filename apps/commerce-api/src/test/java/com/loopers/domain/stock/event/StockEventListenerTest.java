package com.loopers.domain.stock.event;

import com.loopers.domain.coupon.event.CouponEvents;
import com.loopers.domain.order.event.OrderEvents;
import com.loopers.domain.payment.event.PaymentEvents;
import com.loopers.domain.stock.StockService;
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

@DisplayName("StockEventListener 단위 테스트 (Mock 사용)")
@ExtendWith(MockitoExtension.class)
class StockEventListenerTest {

    @Mock
    private StockService stockService;

    @Mock
    private StockEventPublisher stockEventPublisher;

    @InjectMocks
    private StockEventListener stockEventListener;

    private OrderEvents.Created orderCreatedEvent;
    private CouponEvents.ProcessingFailed couponProcessingFailedEvent;
    private PaymentEvents.ProcessingFailed paymentProcessingFailedEvent;

    @BeforeEach
    void setUp() {
        // 테스트용 OrderEvents.Created 생성
        OrderDto.CreateOrderRequest request = OrderDto.CreateOrderRequest.builder()
                .items(List.of(
                        OrderDto.OrderItemRequest.builder()
                                .productId(1L)
                                .quantity(2)
                                .build(),
                        OrderDto.OrderItemRequest.builder()
                                .productId(2L)
                                .quantity(3)
                                .build()
                ))
                .couponIds(List.of())
                .build();

        orderCreatedEvent = new OrderEvents.Created(
                1L, // userId
                100L, // orderId
                BigDecimal.valueOf(50000), // totalPrice
                request
        );

        // 테스트용 StockEvents.Processed 생성 (보상 트랜잭션용)
        StockEvents.Processed stockProcessedEvent = new StockEvents.Processed(
                100L, // orderId
                List.of(
                        new StockEvents.OrderItemInfo(1L, 2),
                        new StockEvents.OrderItemInfo(2L, 3)
                ),
                orderCreatedEvent
        );

        // 테스트용 CouponEvents.ProcessingFailed 생성
        couponProcessingFailedEvent = new CouponEvents.ProcessingFailed(
                100L, // orderId
                stockProcessedEvent, // originalEvent
                "쿠폰 사용 실패"
        );

        // 테스트용 PaymentEvents.ProcessingFailed 생성
        CouponEvents.Processed couponProcessedEvent = new CouponEvents.Processed(
                100L, // orderId
                1L, // userId
                BigDecimal.valueOf(5000), // totalDiscountAmount
                stockProcessedEvent // originalEvent
        );

        paymentProcessingFailedEvent = new PaymentEvents.ProcessingFailed(
                100L, // orderId
                couponProcessedEvent, // originalEvent
                "결제 처리 실패"
        );
    }

    @DisplayName("handleOrderCreated 테스트")
    @Nested
    class HandleOrderCreatedTest {

        @DisplayName("성공 케이스: 재고 차감 성공 시 StockEvents.Processed 발행")
        @Test
        void handleOrderCreated_withValidEvent_publishesStockProcessed() {
            // arrange
            doNothing().when(stockService).decreaseQuantity(anyLong(), anyLong());

            // act
            stockEventListener.handleOrderCreated(orderCreatedEvent);

            // assert
            verify(stockService, times(2)).decreaseQuantity(anyLong(), anyLong());
            verify(stockService).decreaseQuantity(1L, 2L);
            verify(stockService).decreaseQuantity(2L, 3L);
            verify(stockEventPublisher).publishStockProcessed(any(StockEvents.Processed.class));
            verify(stockEventPublisher, never()).publishStockProcessingFailed(any());
        }

        @DisplayName("실패 케이스: 재고 차감 실패 시 StockEvents.ProcessingFailed 발행")
        @Test
        void handleOrderCreated_withStockServiceException_publishesStockProcessingFailed() {
            // arrange
            doThrow(new RuntimeException("재고 부족")).when(stockService).decreaseQuantity(anyLong(), anyLong());

            // act
            stockEventListener.handleOrderCreated(orderCreatedEvent);

            // assert
            verify(stockService, atLeastOnce()).decreaseQuantity(anyLong(), anyLong());
            verify(stockEventPublisher, never()).publishStockProcessed(any());
            verify(stockEventPublisher).publishStockProcessingFailed(any(StockEvents.ProcessingFailed.class));
        }
    }

    @DisplayName("handleCouponProcessingFailed 테스트")
    @Nested
    class HandleCouponProcessingFailedTest {

        @DisplayName("성공 케이스: 재고 원복 성공 시 StockEvents.Compensated 발행")
        @Test
        void handleCouponProcessingFailed_withValidEvent_compensatesStock() {
            // arrange
            doNothing().when(stockService).increaseQuantity(anyLong(), anyLong());

            // act
            stockEventListener.handleCouponProcessingFailed(couponProcessingFailedEvent);

            // assert
            verify(stockService, times(2)).increaseQuantity(anyLong(), anyLong());
            verify(stockService).increaseQuantity(1L, 2L);
            verify(stockService).increaseQuantity(2L, 3L);
            verify(stockEventPublisher).publishStockCompensated(any(StockEvents.Compensated.class));
        }

        @DisplayName("실패 케이스: originalEvent가 null인 경우 재고 원복하지 않음")
        @Test
        void handleCouponProcessingFailed_withNullOriginalEvent_skipsCompensation() {
            // arrange
            CouponEvents.ProcessingFailed eventWithNullOriginal = new CouponEvents.ProcessingFailed(
                    100L, // orderId
                    null, // originalEvent
                    "쿠폰 사용 실패"
            );

            // act
            stockEventListener.handleCouponProcessingFailed(eventWithNullOriginal);

            // assert
            verify(stockService, never()).increaseQuantity(anyLong(), anyLong());
            verify(stockEventPublisher, never()).publishStockCompensated(any());
        }
    }

    @DisplayName("handlePaymentProcessingFailed 테스트")
    @Nested
    class HandlePaymentProcessingFailedTest {

        @DisplayName("성공 케이스: 재고 원복 성공 시 StockEvents.Compensated 발행")
        @Test
        void handlePaymentProcessingFailed_withValidEvent_compensatesStock() {
            // arrange
            doNothing().when(stockService).increaseQuantity(anyLong(), anyLong());

            // act
            stockEventListener.handlePaymentProcessingFailed(paymentProcessingFailedEvent);

            // assert
            verify(stockService, times(2)).increaseQuantity(anyLong(), anyLong());
            verify(stockService).increaseQuantity(1L, 2L);
            verify(stockService).increaseQuantity(2L, 3L);
            verify(stockEventPublisher).publishStockCompensated(any(StockEvents.Compensated.class));
        }

        @DisplayName("실패 케이스: originalEvent가 null인 경우 재고 원복하지 않음")
        @Test
        void handlePaymentProcessingFailed_withNullOriginalEvent_skipsCompensation() {
            // arrange
            PaymentEvents.ProcessingFailed eventWithNullOriginal = new PaymentEvents.ProcessingFailed(
                    100L, // orderId
                    null, // originalEvent
                    "결제 처리 실패"
            );

            // act
            stockEventListener.handlePaymentProcessingFailed(eventWithNullOriginal);

            // assert
            verify(stockService, never()).increaseQuantity(anyLong(), anyLong());
            verify(stockEventPublisher, never()).publishStockCompensated(any());
        }

        @DisplayName("실패 케이스: originalEvent.originalEvent가 null인 경우 재고 원복하지 않음")
        @Test
        void handlePaymentProcessingFailed_withNullNestedOriginalEvent_skipsCompensation() {
            // arrange
            PaymentEvents.ProcessingFailed eventWithNullNestedOriginal = new PaymentEvents.ProcessingFailed(
                    100L, // orderId
                    new CouponEvents.Processed(
                            100L,
                            1L,
                            BigDecimal.valueOf(5000),
                            null // originalEvent가 null
                    ),
                    "결제 처리 실패"
            );

            // act
            stockEventListener.handlePaymentProcessingFailed(eventWithNullNestedOriginal);

            // assert
            verify(stockService, never()).increaseQuantity(anyLong(), anyLong());
            verify(stockEventPublisher, never()).publishStockCompensated(any());
        }
    }
}
