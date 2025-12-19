package com.loopers.interfaces.consumer;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.domain.coupon.event.CouponEvents;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.order.event.OrderEvents;
import com.loopers.domain.payment.PaymentDto;
import com.loopers.domain.payment.event.PaymentEvents;
import com.loopers.domain.stock.event.StockEvents;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@DisplayName("OrderEventListener 통합 테스트")
@SpringBootTest
class OrderEventConsumerIntegrationTest {

    @Autowired
    private KafkaOrderEventConsumer orderConsumer;

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserFacade userFacade;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private Acknowledgment acknowledgment;
    private Long testUserId;
    private final String testLoginId = "order_test_user";

    // ConsumerRecord 헬퍼 메서드
    private <T> ConsumerRecord<String, T> createConsumerRecord(String topic, T value) {
        return new ConsumerRecord<>(topic, 0, 0L, "key", value);
    }

    @BeforeEach
    void setUp() {
        acknowledgment = mock(Acknowledgment.class);

        // 테스트용 User 생성
        UserInfo userInfo = UserInfo.builder()
                .loginId(testLoginId)
                .email("order_test@test.com")
                .birthday("1990-01-01")
                .gender(Gender.MALE)
                .build();
        userFacade.saveUser(userInfo);

        testUserId = userRepository.findByLoginId(testLoginId)
                .orElseThrow(() -> new RuntimeException("User를 찾을 수 없습니다"))
                .getId();
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("handlePaymentProcessed 테스트")
    @Nested
    class HandlePaymentProcessedTest {

        @DisplayName("성공 케이스: 주문 상태 CONFIRMED로 변경")
        @Test
        void handlePaymentProcessed_withValidEvent_confirmsOrder() throws InterruptedException {
            // arrange - Order 생성
            Order order = Order.builder()
                    .userId(testUserId)
                    .discountAmount(BigDecimal.ZERO)
                    .shippingFee(BigDecimal.ZERO)
                    .build();
            order.addOrderItem(1L, "Test Product", BigDecimal.valueOf(10000), 1);
            Order savedOrder = orderService.saveOrder(order);
            Long orderId = savedOrder.getId();

            PaymentEvents.Processed paymentProcessedEvent = new PaymentEvents.Processed(
                    orderId,
                    testUserId,
                    BigDecimal.valueOf(10000),
                    null // originalEvent (PG 콜백 경로)
            );

            ConsumerRecord<String, PaymentEvents.Processed> record = 
                    createConsumerRecord("payment.v1", paymentProcessedEvent);

            // act
            orderConsumer.handlePaymentProcessed(record, acknowledgment);
            waitForAsyncProcessing(1000);

            // assert - 주문 상태가 CONFIRMED로 변경되었는지 확인
            Order confirmedOrder = orderService.findOrderById(orderId);
            assertEquals(OrderStatus.CONFIRMED, confirmedOrder.getOrderStatus(),
                    "주문 상태가 CONFIRMED로 변경되어야 함");
        }

        @DisplayName("멱등성 테스트: 동일한 eventId를 가진 이벤트를 두 번 전송해도 주문 상태가 한 번만 변경됨")
        @Test
        void handlePaymentProcessed_withDuplicateEventId_processesOnlyOnce() throws InterruptedException {
            // arrange - Order 생성
            Order order = Order.builder()
                    .userId(testUserId)
                    .discountAmount(BigDecimal.ZERO)
                    .shippingFee(BigDecimal.ZERO)
                    .build();
            order.addOrderItem(1L, "Test Product", BigDecimal.valueOf(10000), 1);
            Order savedOrder = orderService.saveOrder(order);
            Long orderId = savedOrder.getId();

            // 동일한 eventId를 가진 이벤트 생성
            String duplicateEventId = UUID.randomUUID().toString();
            LocalDateTime occurredAt = LocalDateTime.now();

            PaymentEvents.Processed paymentProcessedEvent1 = new PaymentEvents.Processed(
                    duplicateEventId,
                    orderId,
                    testUserId,
                    BigDecimal.valueOf(10000),
                    null,
                    occurredAt
            );

            PaymentEvents.Processed paymentProcessedEvent2 = new PaymentEvents.Processed(
                    duplicateEventId, // 동일한 eventId
                    orderId,
                    testUserId,
                    BigDecimal.valueOf(10000),
                    null,
                    occurredAt
            );

            ConsumerRecord<String, PaymentEvents.Processed> record1 = 
                    createConsumerRecord("payment.v1", paymentProcessedEvent1);
            ConsumerRecord<String, PaymentEvents.Processed> record2 = 
                    createConsumerRecord("payment.v1", paymentProcessedEvent2);

            // act - 첫 번째 이벤트 처리
            orderConsumer.handlePaymentProcessed(record1, acknowledgment);
            waitForAsyncProcessing(1000);

            // 첫 번째 이벤트 처리 후 주문 상태 확인
            Order orderAfterFirst = orderService.findOrderById(orderId);
            assertEquals(OrderStatus.CONFIRMED, orderAfterFirst.getOrderStatus(),
                    "첫 번째 이벤트 처리 후 주문 상태가 CONFIRMED로 변경되어야 함");

            // act - 두 번째 이벤트 처리 (동일한 eventId)
            orderConsumer.handlePaymentProcessed(record2, acknowledgment);
            waitForAsyncProcessing(1000);

            // assert - 주문 상태가 추가로 변경되지 않아야 함 (멱등성 보장)
            Order orderAfterSecond = orderService.findOrderById(orderId);
            assertEquals(OrderStatus.CONFIRMED, orderAfterSecond.getOrderStatus(),
                    "두 번째 이벤트는 무시되어야 하므로 주문 상태가 변경되지 않아야 함");
        }
    }

    @DisplayName("실패 핸들러 테스트")
    @Nested
    class FailureHandlerTest {

        @DisplayName("성공 케이스: 재고 처리 실패 이벤트 처리 시 주문 상태 PAYMENT_FAILED로 변경")
        @Test
        void handleStockProcessingFailed_updatesOrderStatusToFailed() throws InterruptedException {
            // arrange - Order 생성
            Order order = Order.builder()
                    .userId(testUserId)
                    .discountAmount(BigDecimal.ZERO)
                    .shippingFee(BigDecimal.ZERO)
                    .build();
            order.addOrderItem(1L, "Test Product", BigDecimal.valueOf(10000), 1);
            Order savedOrder = orderService.saveOrder(order);
            Long orderId = savedOrder.getId();

            StockEvents.ProcessingFailed stockProcessingFailedEvent = new StockEvents.ProcessingFailed(
                    orderId,
                    List.of(),
                    "재고 부족"
            );

            ConsumerRecord<String, StockEvents.ProcessingFailed> record = 
                    createConsumerRecord("stock.v1", stockProcessingFailedEvent);

            // act
            orderConsumer.handleStockProcessingFailed(record, acknowledgment);
            waitForAsyncProcessing(1000);

            // assert - 주문 상태가 PAYMENT_FAILED로 변경되었는지 확인
            Order failedOrder = orderService.findOrderById(orderId);
            assertEquals(OrderStatus.PAYMENT_FAILED, failedOrder.getOrderStatus(),
                    "주문 상태가 PAYMENT_FAILED로 변경되어야 함");
        }

        @DisplayName("성공 케이스: 쿠폰 처리 실패 이벤트 처리 시 주문 상태 PAYMENT_FAILED로 변경")
        @Test
        void handleCouponProcessingFailed_updatesOrderStatusToFailed() throws InterruptedException {
            // arrange - Order 생성
            Order order = Order.builder()
                    .userId(testUserId)
                    .discountAmount(BigDecimal.ZERO)
                    .shippingFee(BigDecimal.ZERO)
                    .build();
            order.addOrderItem(1L, "Test Product", BigDecimal.valueOf(10000), 1);
            Order savedOrder = orderService.saveOrder(order);
            Long orderId = savedOrder.getId();

            CouponEvents.ProcessingFailed couponProcessingFailedEvent = new CouponEvents.ProcessingFailed(
                    orderId,
                    null, // originalEvent
                    "쿠폰 사용 실패"
            );

            ConsumerRecord<String, CouponEvents.ProcessingFailed> record = 
                    createConsumerRecord("coupon.v1", couponProcessingFailedEvent);

            // act
            orderConsumer.handleCouponProcessingFailed(record, acknowledgment);
            waitForAsyncProcessing(1000);

            // assert - 주문 상태가 PAYMENT_FAILED로 변경되었는지 확인
            Order failedOrder = orderService.findOrderById(orderId);
            assertEquals(OrderStatus.PAYMENT_FAILED, failedOrder.getOrderStatus(),
                    "주문 상태가 PAYMENT_FAILED로 변경되어야 함");
        }

        @DisplayName("성공 케이스: 결제 처리 실패 이벤트 처리 시 주문 상태 PAYMENT_FAILED로 변경")
        @Test
        void handlePaymentProcessingFailed_updatesOrderStatusToFailed() throws InterruptedException {
            // arrange - Order 생성
            Order order = Order.builder()
                    .userId(testUserId)
                    .discountAmount(BigDecimal.ZERO)
                    .shippingFee(BigDecimal.ZERO)
                    .build();
            order.addOrderItem(1L, "Test Product", BigDecimal.valueOf(10000), 1);
            Order savedOrder = orderService.saveOrder(order);
            Long orderId = savedOrder.getId();

            PaymentEvents.ProcessingFailed paymentProcessingFailedEvent = new PaymentEvents.ProcessingFailed(
                    orderId,
                    null, // originalEvent
                    "결제 처리 실패"
            );

            ConsumerRecord<String, PaymentEvents.ProcessingFailed> record = 
                    createConsumerRecord("payment.v1", paymentProcessingFailedEvent);

            // act
            orderConsumer.handlePaymentProcessingFailed(record, acknowledgment);
            waitForAsyncProcessing(1000);

            // assert - 주문 상태가 PAYMENT_FAILED로 변경되었는지 확인
            Order failedOrder = orderService.findOrderById(orderId);
            assertEquals(OrderStatus.PAYMENT_FAILED, failedOrder.getOrderStatus(),
                    "주문 상태가 PAYMENT_FAILED로 변경되어야 함");
        }

        @DisplayName("멱등성 테스트: 동일한 eventId를 가진 실패 이벤트를 두 번 전송해도 주문 상태가 한 번만 변경됨")
        @Test
        void handleFailureEvents_withDuplicateEventId_processesOnlyOnce() throws InterruptedException {
            // arrange - Order 생성
            Order order = Order.builder()
                    .userId(testUserId)
                    .discountAmount(BigDecimal.ZERO)
                    .shippingFee(BigDecimal.ZERO)
                    .build();
            order.addOrderItem(1L, "Test Product", BigDecimal.valueOf(10000), 1);
            Order savedOrder = orderService.saveOrder(order);
            Long orderId = savedOrder.getId();

            // 동일한 eventId를 가진 이벤트 생성
            String duplicateEventId = UUID.randomUUID().toString();
            LocalDateTime occurredAt = LocalDateTime.now();

            StockEvents.ProcessingFailed stockProcessingFailedEvent1 = new StockEvents.ProcessingFailed(
                    duplicateEventId,
                    orderId,
                    List.of(),
                    "재고 부족",
                    occurredAt
            );

            StockEvents.ProcessingFailed stockProcessingFailedEvent2 = new StockEvents.ProcessingFailed(
                    duplicateEventId, // 동일한 eventId
                    orderId,
                    List.of(),
                    "재고 부족",
                    occurredAt
            );

            ConsumerRecord<String, StockEvents.ProcessingFailed> record1 = 
                    createConsumerRecord("stock.v1", stockProcessingFailedEvent1);
            ConsumerRecord<String, StockEvents.ProcessingFailed> record2 = 
                    createConsumerRecord("stock.v1", stockProcessingFailedEvent2);

            // act - 첫 번째 이벤트 처리
            orderConsumer.handleStockProcessingFailed(record1, acknowledgment);
            waitForAsyncProcessing(1000);

            // 첫 번째 이벤트 처리 후 주문 상태 확인
            Order orderAfterFirst = orderService.findOrderById(orderId);
            assertEquals(OrderStatus.PAYMENT_FAILED, orderAfterFirst.getOrderStatus(),
                    "첫 번째 이벤트 처리 후 주문 상태가 PAYMENT_FAILED로 변경되어야 함");

            // act - 두 번째 이벤트 처리 (동일한 eventId)
            orderConsumer.handleStockProcessingFailed(record2, acknowledgment);
            waitForAsyncProcessing(1000);

            // assert - 주문 상태가 추가로 변경되지 않아야 함 (멱등성 보장)
            Order orderAfterSecond = orderService.findOrderById(orderId);
            assertEquals(OrderStatus.PAYMENT_FAILED, orderAfterSecond.getOrderStatus(),
                    "두 번째 이벤트는 무시되어야 하므로 주문 상태가 변경되지 않아야 함");
        }
    }

    /**
     * 비동기 이벤트 핸들러 완료 대기
     */
    private void waitForAsyncProcessing() throws InterruptedException {
        waitForAsyncProcessing(500);
    }

    private void waitForAsyncProcessing(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }
}

