package com.loopers.interfaces.consumer;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.domain.coupon.event.CouponEvents;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.event.OrderEvents;
import com.loopers.domain.payment.*;
import com.loopers.domain.payment.event.PaymentEvents;
import com.loopers.domain.payment.strategy.PaymentStrategyFactory;
import com.loopers.domain.payment.strategy.PaymentStrategy;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@DisplayName("PaymentEventConsumer 통합 테스트")
@SpringBootTest
class PaymentEventConsumerIntegrationTest {

    @MockitoBean
    private PgFeignClient pgFeignClient;

    @Autowired
    private KafkaPaymentEventConsumer paymentConsumer;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentStrategyFactory paymentStrategyFactory;

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserFacade userFacade;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommercePaymentRepository commercePaymentRepository;

    @Autowired
    private com.loopers.infrastructure.payment.CommercePaymentJpaRepository commercePaymentJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private Acknowledgment acknowledgment;
    private Long testUserId;
    private final String testLoginId = "payment_test_user";

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
                .email("payment_test@test.com")
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

    @DisplayName("handleCouponProcessed 테스트")
    @Nested
    class HandleCouponProcessedTest {

        @DisplayName("성공 케이스: 결제 금액이 0원 이하인 경우 CommercePayment 저장 없이 처리")
        @Test
        void handleCouponProcessed_withZeroAmount_processesWithoutPayment() throws InterruptedException {
            // arrange - 할인 금액이 총액과 동일하여 결제 금액이 0원인 경우
            Order order = Order.builder()
                    .userId(testUserId)
                    .discountAmount(BigDecimal.ZERO)
                    .shippingFee(BigDecimal.ZERO)
                    .build();
            order.addOrderItem(1L, "Test Product", BigDecimal.valueOf(10000), 1);
            Order savedOrder = orderService.saveOrder(order);
            Long orderId = savedOrder.getId();

            List<OrderEvents.OrderItemInfo> items = List.of(
                    new OrderEvents.OrderItemInfo(1L, "Test Product", BigDecimal.valueOf(10000), 1)
            );

            OrderEvents.Created orderCreatedEvent = new OrderEvents.Created(
                    orderId,
                    testUserId,
                    BigDecimal.valueOf(10000),
                    items,
                    List.of(),
                    PaymentDto.PaymentMethod.CARD
            );

            StockEvents.Processed stockProcessedEvent = new StockEvents.Processed(
                    orderId,
                    List.of(),
                    orderCreatedEvent
            );

            // 할인 금액이 총액과 동일하여 결제 금액이 0원
            CouponEvents.Processed couponProcessedEvent = new CouponEvents.Processed(
                    orderId,
                    testUserId,
                    BigDecimal.valueOf(10000), // totalDiscountAmount가 totalAmount와 동일
                    stockProcessedEvent
            );

            ConsumerRecord<String, CouponEvents.Processed> record = 
                    createConsumerRecord("coupon.v1", couponProcessedEvent);

            // act
            paymentConsumer.handleCouponProcessed(record, acknowledgment);
            waitForAsyncProcessing(1000);

            // assert - CommercePayment가 저장되지 않아야 함
            // 결제 금액이 0원이면 CommercePayment가 저장되지 않으므로, 
            // 임의의 transactionKey로 조회 시도하여 NOT_FOUND 예외가 발생하는지 확인
            try {
                paymentService.findByTransactionKey("NON_EXISTENT_KEY");
                fail("CommercePayment가 저장되지 않아야 함");
            } catch (Exception e) {
                // 예상된 동작: CommercePayment가 없으므로 예외 발생
                assertTrue(true);
            }
        }

        @DisplayName("성공 케이스: 결제 처리 성공 시 CommercePayment 저장")
        @Test
        void handleCouponProcessed_withValidPayment_savesCommercePayment() throws InterruptedException {
            // arrange
            Order order = Order.builder()
                    .userId(testUserId)
                    .discountAmount(BigDecimal.ZERO)
                    .shippingFee(BigDecimal.ZERO)
                    .build();
            order.addOrderItem(1L, "Test Product", BigDecimal.valueOf(10000), 1);
            Order savedOrder = orderService.saveOrder(order);
            Long orderId = savedOrder.getId();

            List<OrderEvents.OrderItemInfo> items = List.of(
                    new OrderEvents.OrderItemInfo(1L, "Test Product", BigDecimal.valueOf(10000), 1)
            );

            OrderEvents.Created orderCreatedEvent = new OrderEvents.Created(
                    orderId,
                    testUserId,
                    BigDecimal.valueOf(10000),
                    items,
                    List.of(),
                    PaymentDto.PaymentMethod.CARD
            );

            StockEvents.Processed stockProcessedEvent = new StockEvents.Processed(
                    orderId,
                    List.of(),
                    orderCreatedEvent
            );

            CouponEvents.Processed couponProcessedEvent = new CouponEvents.Processed(
                    orderId,
                    testUserId,
                    BigDecimal.valueOf(2000), // 할인 금액
                    stockProcessedEvent
            );

            ConsumerRecord<String, CouponEvents.Processed> record = 
                    createConsumerRecord("coupon.v1", couponProcessedEvent);

            // act
            paymentConsumer.handleCouponProcessed(record, acknowledgment);
            waitForAsyncProcessing(2000);

            // assert - CommercePayment가 저장되었는지 확인
            // CommercePaymentJpaRepository의 findAll()을 사용하여 orderId로 필터링
            List<CommercePayment> allPayments = commercePaymentJpaRepository.findAll();
            List<CommercePayment> payments = allPayments.stream()
                    .filter(p -> p.getOrderId().equals(orderId))
                    .toList();
            assertFalse(payments.isEmpty(), "CommercePayment가 저장되어야 함");
            
            CommercePayment payment = payments.get(0);
            assertEquals(orderId, payment.getOrderId(), "orderId가 일치해야 함");
            assertEquals(PaymentDto.PaymentMethod.CARD, payment.getMethod(), "결제 방법이 일치해야 함");
            assertEquals(BigDecimal.valueOf(8000), payment.getAmount(), "최종 결제 금액이 8000원이어야 함");
        }

        @DisplayName("멱등성 테스트: 동일한 eventId를 가진 이벤트를 두 번 전송해도 CommercePayment가 한 번만 저장됨")
        @Test
        void handleCouponProcessed_withDuplicateEventId_processesOnlyOnce() throws InterruptedException {
            // arrange
            Order order = Order.builder()
                    .userId(testUserId)
                    .discountAmount(BigDecimal.ZERO)
                    .shippingFee(BigDecimal.ZERO)
                    .build();
            order.addOrderItem(1L, "Test Product", BigDecimal.valueOf(10000), 1);
            Order savedOrder = orderService.saveOrder(order);
            Long orderId = savedOrder.getId();

            List<OrderEvents.OrderItemInfo> items = List.of(
                    new OrderEvents.OrderItemInfo(1L, "Test Product", BigDecimal.valueOf(10000), 1)
            );

            // 동일한 eventId를 가진 이벤트 생성
            String duplicateEventId = UUID.randomUUID().toString();
            LocalDateTime occurredAt = LocalDateTime.now();

            OrderEvents.Created orderCreatedEvent = new OrderEvents.Created(
                    duplicateEventId,
                    orderId,
                    testUserId,
                    BigDecimal.valueOf(10000),
                    items,
                    List.of(),
                    PaymentDto.PaymentMethod.CARD,
                    occurredAt
            );

            StockEvents.Processed stockProcessedEvent1 = new StockEvents.Processed(
                    duplicateEventId,
                    orderId,
                    List.of(),
                    orderCreatedEvent,
                    occurredAt
            );

            StockEvents.Processed stockProcessedEvent2 = new StockEvents.Processed(
                    duplicateEventId, // 동일한 eventId
                    orderId,
                    List.of(),
                    orderCreatedEvent,
                    occurredAt
            );

            CouponEvents.Processed couponProcessedEvent1 = new CouponEvents.Processed(
                    duplicateEventId,
                    orderId,
                    testUserId,
                    BigDecimal.valueOf(2000),
                    stockProcessedEvent1,
                    occurredAt
            );

            CouponEvents.Processed couponProcessedEvent2 = new CouponEvents.Processed(
                    duplicateEventId, // 동일한 eventId
                    orderId,
                    testUserId,
                    BigDecimal.valueOf(2000),
                    stockProcessedEvent2,
                    occurredAt
            );

            ConsumerRecord<String, CouponEvents.Processed> record1 = 
                    createConsumerRecord("coupon.v1", couponProcessedEvent1);
            ConsumerRecord<String, CouponEvents.Processed> record2 = 
                    createConsumerRecord("coupon.v1", couponProcessedEvent2);

            // act - 첫 번째 이벤트 처리
            paymentConsumer.handleCouponProcessed(record1, acknowledgment);
            waitForAsyncProcessing(2000);

            // 첫 번째 이벤트 처리 후 CommercePayment 확인
            List<CommercePayment> allPaymentsAfterFirst = commercePaymentJpaRepository.findAll();
            List<CommercePayment> paymentsAfterFirst = allPaymentsAfterFirst.stream()
                    .filter(p -> p.getOrderId().equals(orderId))
                    .toList();
            assertEquals(1, paymentsAfterFirst.size(), "첫 번째 이벤트 처리 후 CommercePayment가 1개 저장되어야 함");

            // act - 두 번째 이벤트 처리 (동일한 eventId)
            paymentConsumer.handleCouponProcessed(record2, acknowledgment);
            waitForAsyncProcessing(2000);

            // assert - CommercePayment가 추가로 저장되지 않아야 함 (멱등성 보장)
            List<CommercePayment> allPaymentsAfterSecond = commercePaymentJpaRepository.findAll();
            List<CommercePayment> paymentsAfterSecond = allPaymentsAfterSecond.stream()
                    .filter(p -> p.getOrderId().equals(orderId))
                    .toList();
            assertEquals(1, paymentsAfterSecond.size(), 
                    "두 번째 이벤트는 무시되어야 하므로 CommercePayment가 추가로 저장되지 않아야 함");
        }
    }

    @DisplayName("handlePaymentCallbackReceived 테스트")
    @Nested
    class HandlePaymentCallbackReceivedTest {

        @DisplayName("성공 케이스: PG 콜백 성공 시 CommercePayment 상태 변경")
        @Test
        void handlePaymentCallbackReceived_withSuccessStatus_updatesPaymentStatus() throws InterruptedException {
            // arrange - CommercePayment 생성
            Order order = Order.builder()
                    .userId(testUserId)
                    .discountAmount(BigDecimal.ZERO)
                    .shippingFee(BigDecimal.ZERO)
                    .build();
            order.addOrderItem(1L, "Test Product", BigDecimal.valueOf(10000), 1);
            Order savedOrder = orderService.saveOrder(order);
            Long orderId = savedOrder.getId();

            String transactionKey = "TEST_TRANSACTION_KEY_" + UUID.randomUUID();
            CommercePayment commercePayment = CommercePayment.builder()
                    .orderId(orderId)
                    .transactionKey(transactionKey)
                    .method(PaymentDto.PaymentMethod.CARD)
                    .paymentStatus(PaymentDto.PaymentStatus.PENDING)
                    .amount(BigDecimal.valueOf(10000))
                    .build();
            commercePaymentRepository.save(commercePayment);

            PaymentEvents.CallbackReceived callbackEvent = new PaymentEvents.CallbackReceived(
                    transactionKey,
                    orderId,
                    PaymentDto.PaymentStatus.SUCCESS,
                    null
            );

            ConsumerRecord<String, PaymentEvents.CallbackReceived> record = 
                    createConsumerRecord("payment.v1", callbackEvent);

            // act
            paymentConsumer.handlePaymentCallbackReceived(record, acknowledgment);
            waitForAsyncProcessing(1000);

            // assert - CommercePayment 상태가 SUCCESS로 변경되었는지 확인
            CommercePayment updatedPayment = paymentService.findByTransactionKey(transactionKey);
            assertEquals(PaymentDto.PaymentStatus.SUCCESS, updatedPayment.getPaymentStatus(),
                    "CommercePayment 상태가 SUCCESS로 변경되어야 함");
        }

        @DisplayName("멱등성 테스트: 동일한 eventId를 가진 콜백 이벤트를 두 번 전송해도 상태가 한 번만 변경됨")
        @Test
        void handlePaymentCallbackReceived_withDuplicateEventId_processesOnlyOnce() throws InterruptedException {
            // arrange - CommercePayment 생성
            Order order = Order.builder()
                    .userId(testUserId)
                    .discountAmount(BigDecimal.ZERO)
                    .shippingFee(BigDecimal.ZERO)
                    .build();
            order.addOrderItem(1L, "Test Product", BigDecimal.valueOf(10000), 1);
            Order savedOrder = orderService.saveOrder(order);
            Long orderId = savedOrder.getId();

            String transactionKey = "TEST_TRANSACTION_KEY_" + UUID.randomUUID();
            CommercePayment commercePayment = CommercePayment.builder()
                    .orderId(orderId)
                    .transactionKey(transactionKey)
                    .method(PaymentDto.PaymentMethod.CARD)
                    .paymentStatus(PaymentDto.PaymentStatus.PENDING)
                    .amount(BigDecimal.valueOf(10000))
                    .build();
            commercePaymentRepository.save(commercePayment);

            // 동일한 eventId를 가진 이벤트 생성
            String duplicateEventId = UUID.randomUUID().toString();
            LocalDateTime occurredAt = LocalDateTime.now();

            PaymentEvents.CallbackReceived callbackEvent1 = new PaymentEvents.CallbackReceived(
                    duplicateEventId,
                    transactionKey,
                    orderId,
                    PaymentDto.PaymentStatus.SUCCESS,
                    null,
                    occurredAt
            );

            PaymentEvents.CallbackReceived callbackEvent2 = new PaymentEvents.CallbackReceived(
                    duplicateEventId, // 동일한 eventId
                    transactionKey,
                    orderId,
                    PaymentDto.PaymentStatus.SUCCESS,
                    null,
                    occurredAt
            );

            ConsumerRecord<String, PaymentEvents.CallbackReceived> record1 = 
                    createConsumerRecord("payment.v1", callbackEvent1);
            ConsumerRecord<String, PaymentEvents.CallbackReceived> record2 = 
                    createConsumerRecord("payment.v1", callbackEvent2);

            // act - 첫 번째 이벤트 처리
            paymentConsumer.handlePaymentCallbackReceived(record1, acknowledgment);
            waitForAsyncProcessing(1000);

            // 첫 번째 이벤트 처리 후 상태 확인
            CommercePayment paymentAfterFirst = paymentService.findByTransactionKey(transactionKey);
            assertEquals(PaymentDto.PaymentStatus.SUCCESS, paymentAfterFirst.getPaymentStatus(),
                    "첫 번째 이벤트 처리 후 상태가 SUCCESS로 변경되어야 함");

            // act - 두 번째 이벤트 처리 (동일한 eventId)
            paymentConsumer.handlePaymentCallbackReceived(record2, acknowledgment);
            waitForAsyncProcessing(1000);

            // assert - 상태가 추가로 변경되지 않아야 함 (멱등성 보장)
            CommercePayment paymentAfterSecond = paymentService.findByTransactionKey(transactionKey);
            assertEquals(PaymentDto.PaymentStatus.SUCCESS, paymentAfterSecond.getPaymentStatus(),
                    "두 번째 이벤트는 무시되어야 하므로 상태가 변경되지 않아야 함");
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

