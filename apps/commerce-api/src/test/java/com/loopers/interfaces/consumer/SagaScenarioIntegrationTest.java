package com.loopers.interfaces.consumer;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandStatus;
import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponRepository;
import com.loopers.domain.coupon.CouponType;
import com.loopers.domain.coupon.event.CouponEventPublisher;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderRepository;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.payment.PaymentDto;
import com.loopers.domain.payment.PgFeignClient;
import com.loopers.domain.payment.event.PaymentEventPublisher;
import com.loopers.domain.payment.event.PaymentEvents;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductStatus;
import com.loopers.domain.stock.Stock;
import com.loopers.domain.stock.StockRepository;
import com.loopers.domain.stock.StockService;
import com.loopers.domain.stock.event.StockEventPublisher;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserRepository;
import com.loopers.infrastructure.brand.BrandJpaRepository;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * SAGA 패턴 시나리오 통합 테스트
 * 전체 주문 플로우(Order -> Stock -> Coupon -> Payment)와 보상 트랜잭션을 검증합니다.
 */
@DisplayName("SAGA 시나리오 통합 테스트")
@SpringBootTest
class SagaScenarioIntegrationTest {

    @Autowired
    private KafkaStockEventConsumer stockConsumer;

    @Autowired
    private KafkaCouponEventConsumer couponConsumer;

    @Autowired
    private KafkaPaymentEventConsumer paymentConsumer;

    @Autowired
    private KafkaOrderEventConsumer orderConsumer;

    @MockitoBean
    private StockEventPublisher stockEventPublisher;

    @MockitoBean
    private CouponEventPublisher couponEventPublisher;

    @MockitoBean
    private PaymentEventPublisher paymentEventPublisher;

    @MockitoBean
    private PgFeignClient pgFeignClient;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private StockService stockService;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandJpaRepository brandJpaRepository;

    @Autowired
    private UserFacade userFacade;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private Acknowledgment acknowledgment;

    private Long testUserId;
    private Long testProductId;
    private Long testCouponId;
    private final Long initialStockQuantity = 100L;
    private final String testLoginId = "saga1";

    private <T> ConsumerRecord<String, T> createConsumerRecord(String topic, T value) {
        return new ConsumerRecord<>(topic, 0, 0L, "key", value);
    }

    @BeforeEach
    void setUp() {
        acknowledgment = mock(Acknowledgment.class);

        // 테스트용 User 생성
        UserInfo userInfo = UserInfo.builder()
                .loginId(testLoginId)
                .email("saga1@test.com")
                .birthday("1990-01-01")
                .gender(Gender.MALE)
                .build();
        userFacade.saveUser(userInfo);

        testUserId = userRepository.findByLoginId(testLoginId)
                .orElseThrow(() -> new RuntimeException("User를 찾을 수 없습니다"))
                .getId();

        // 테스트용 Brand 생성
        Brand brand = Brand.builder()
                .name("SAGA Test Brand")
                .description("Test Description")
                .status(BrandStatus.ON_SALE)
                .isVisible(true)
                .isSellable(true)
                .build();
        Brand savedBrand = brandJpaRepository.save(brand);

        // 테스트용 Product 생성
        Product product = Product.builder()
                .name("SAGA Test Product")
                .description("Test Description")
                .price(BigDecimal.valueOf(10000))
                .status(ProductStatus.ON_SALE)
                .isVisible(true)
                .isSellable(true)
                .brandId(savedBrand.getId())
                .build();
        Product savedProduct = productRepository.save(product)
                .orElseThrow(() -> new RuntimeException("Product 저장 실패"));
        testProductId = savedProduct.getId();

        // Stock 생성 및 초기 재고 설정
        Stock stock = Stock.builder()
                .productId(testProductId)
                .quantity(0L)
                .build();
        stockService.saveStock(stock)
                .orElseThrow(() -> new RuntimeException("Stock 저장 실패"));
        stockService.increaseQuantity(testProductId, initialStockQuantity);

        // 테스트용 Coupon 생성
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(5000))
                .userId(testUserId)
                .build();
        Coupon savedCoupon = couponRepository.save(coupon)
                .orElseThrow(() -> new RuntimeException("Coupon 저장 실패"));
        testCouponId = savedCoupon.getId();
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("정상 시나리오: 주문 생성부터 결제 완료까지 전체 플로우")
    @Nested
    class SuccessScenarioTest {

        @DisplayName("성공 케이스: Order -> Stock -> Coupon -> Payment 전체 플로우 성공")
        @Test
        void fullOrderFlow_success_allStepsCompleted() throws InterruptedException {
            // arrange
            Order order = Order.builder()
                    .userId(testUserId)
                    .discountAmount(BigDecimal.ZERO)
                    .shippingFee(BigDecimal.ZERO)
                    .build();
            order.addOrderItem(testProductId, "SAGA Test Product", BigDecimal.valueOf(10000), 5);
            Order savedOrder = orderService.saveOrder(order);
            Long orderId = savedOrder.getId();

            // OrderEvents.Created 이벤트 생성 및 처리
            com.loopers.domain.order.event.OrderEvents.Created orderCreatedEvent = 
                    new com.loopers.domain.order.event.OrderEvents.Created(
                            orderId,
                            testUserId,
                            BigDecimal.valueOf(50000),
                            List.of(new com.loopers.domain.order.event.OrderEvents.OrderItemInfo(
                                    testProductId, "SAGA Test Product", BigDecimal.valueOf(10000), 5)),
                            List.of(testCouponId),
                            PaymentDto.PaymentMethod.CARD
                    );

            ConsumerRecord<String, com.loopers.domain.order.event.OrderEvents.Created> orderRecord = 
                    createConsumerRecord("order.v1", orderCreatedEvent);

            // act - Step 1: Stock 차감
            stockConsumer.handleOrderCreated(orderRecord, acknowledgment);
            waitForAsyncProcessing(1000);

            // assert - 재고가 차감되었는지 확인
            Stock stock = stockRepository.findByProductId(testProductId)
                    .orElseThrow(() -> new RuntimeException("Stock을 찾을 수 없습니다"));
            assertEquals(initialStockQuantity - 5L, stock.getQuantity(),
                    "재고가 5개 차감되어야 함");

            // Step 2: StockProcessed 이벤트 생성 및 Coupon 처리
            com.loopers.domain.stock.event.StockEvents.Processed stockProcessedEvent = 
                    new com.loopers.domain.stock.event.StockEvents.Processed(
                            orderId,
                            List.of(new com.loopers.domain.stock.event.StockEvents.OrderItemInfo(testProductId, 5)),
                            orderCreatedEvent
                    );

            ConsumerRecord<String, com.loopers.domain.stock.event.StockEvents.Processed> stockRecord = 
                    createConsumerRecord("stock.v1", stockProcessedEvent);

            couponConsumer.handleStockProcessed(stockRecord, acknowledgment);
            waitForAsyncProcessing(2000);

            // assert - 쿠폰이 사용되었는지 확인
            Coupon usedCoupon = couponRepository.findById(testCouponId)
                    .orElseThrow(() -> new RuntimeException("Coupon을 찾을 수 없습니다"));
            assertTrue(usedCoupon.getIsUsed(), "쿠폰이 사용되어야 함");
            assertEquals(orderId, usedCoupon.getOrderId(), "쿠폰의 orderId가 설정되어야 함");

            // Step 3: CouponProcessed 이벤트 생성 및 Payment 처리
            com.loopers.domain.coupon.event.CouponEvents.Processed couponProcessedEvent = 
                    new com.loopers.domain.coupon.event.CouponEvents.Processed(
                            orderId,
                            testUserId,
                            BigDecimal.valueOf(5000),
                            stockProcessedEvent
                    );

            ConsumerRecord<String, com.loopers.domain.coupon.event.CouponEvents.Processed> couponRecord = 
                    createConsumerRecord("coupon.v1", couponProcessedEvent);

            // PgFeignClient Mock 설정 - 성공 응답 반환
            String transactionKey = "TEST_TXN_KEY_" + UUID.randomUUID();
            PaymentDto.PgResponse pgResponse = new PaymentDto.PgResponse(
                    transactionKey,
                    PaymentDto.PaymentStatus.PENDING,
                    null
            );
            when(pgFeignClient.approvePayment(anyLong(), any(PaymentDto.PgRequest.class)))
                    .thenReturn(ApiResponse.success(pgResponse));

            paymentConsumer.handleCouponProcessed(couponRecord, acknowledgment);
            waitForAsyncProcessing(2000);

            // PaymentEvents.Processed 이벤트 생성 및 처리 (Outbox 패턴으로 발행된 이벤트를 테스트에서 직접 처리)
            BigDecimal finalAmount = BigDecimal.valueOf(50000).subtract(BigDecimal.valueOf(5000)); // totalPrice - discountAmount
            PaymentEvents.Processed paymentProcessedEvent = new PaymentEvents.Processed(
                    orderId,
                    testUserId,
                    finalAmount,
                    couponProcessedEvent
            );
            ConsumerRecord<String, PaymentEvents.Processed> paymentProcessedRecord = 
                    createConsumerRecord("payment.v1", paymentProcessedEvent);
            
            orderConsumer.handlePaymentProcessed(paymentProcessedRecord, acknowledgment);
            waitForAsyncProcessing(2000);

            // assert - 주문 상태가 CONFIRMED로 변경되었는지 확인
            Order confirmedOrder = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order를 찾을 수 없습니다"));
            assertEquals(OrderStatus.CONFIRMED, confirmedOrder.getOrderStatus(),
                    "주문 상태가 CONFIRMED로 변경되어야 함");
        }
    }

    @DisplayName("보상 트랜잭션 시나리오: 결제 실패 시 재고 및 쿠폰 원복")
    @Nested
    class CompensationScenarioTest {

        @DisplayName("성공 케이스: 결제 실패 시 재고와 쿠폰이 원복됨")
        @Test
        void paymentFailure_compensatesStockAndCoupon() throws InterruptedException {
            // arrange
            Order order = Order.builder()
                    .userId(testUserId)
                    .discountAmount(BigDecimal.ZERO)
                    .shippingFee(BigDecimal.ZERO)
                    .build();
            order.addOrderItem(testProductId, "SAGA Test Product", BigDecimal.valueOf(10000), 5);
            Order savedOrder = orderService.saveOrder(order);
            Long orderId = savedOrder.getId();

            // Step 1: Stock 차감
            com.loopers.domain.order.event.OrderEvents.Created orderCreatedEvent = 
                    new com.loopers.domain.order.event.OrderEvents.Created(
                            orderId,
                            testUserId,
                            BigDecimal.valueOf(50000),
                            List.of(new com.loopers.domain.order.event.OrderEvents.OrderItemInfo(
                                    testProductId, "SAGA Test Product", BigDecimal.valueOf(10000), 5)),
                            List.of(testCouponId),
                            PaymentDto.PaymentMethod.CARD
                    );

            ConsumerRecord<String, com.loopers.domain.order.event.OrderEvents.Created> orderRecord = 
                    createConsumerRecord("order.v1", orderCreatedEvent);

            stockConsumer.handleOrderCreated(orderRecord, acknowledgment);
            waitForAsyncProcessing(1000);

            // 재고 차감 확인
            Stock stockBefore = stockRepository.findByProductId(testProductId)
                    .orElseThrow(() -> new RuntimeException("Stock을 찾을 수 없습니다"));
            assertEquals(initialStockQuantity - 5L, stockBefore.getQuantity(),
                    "재고가 5개 차감되어야 함");

            // Step 2: Coupon 사용
            com.loopers.domain.stock.event.StockEvents.Processed stockProcessedEvent = 
                    new com.loopers.domain.stock.event.StockEvents.Processed(
                            orderId,
                            List.of(new com.loopers.domain.stock.event.StockEvents.OrderItemInfo(testProductId, 5)),
                            orderCreatedEvent
                    );

            ConsumerRecord<String, com.loopers.domain.stock.event.StockEvents.Processed> stockRecord = 
                    createConsumerRecord("stock.v1", stockProcessedEvent);

            couponConsumer.handleStockProcessed(stockRecord, acknowledgment);
            waitForAsyncProcessing(2000);

            // 쿠폰 사용 확인
            Coupon couponBefore = couponRepository.findById(testCouponId)
                    .orElseThrow(() -> new RuntimeException("Coupon을 찾을 수 없습니다"));
            assertTrue(couponBefore.getIsUsed(), "쿠폰이 사용되어야 함");

            // Step 3: Payment 실패 이벤트 처리 (보상 트랜잭션 시작)
            com.loopers.domain.coupon.event.CouponEvents.Processed couponProcessedEvent = 
                    new com.loopers.domain.coupon.event.CouponEvents.Processed(
                            orderId,
                            testUserId,
                            BigDecimal.valueOf(5000),
                            stockProcessedEvent
                    );

            PaymentEvents.ProcessingFailed paymentFailedEvent = new PaymentEvents.ProcessingFailed(
                    orderId,
                    couponProcessedEvent,
                    "결제 처리 실패"
            );

            ConsumerRecord<String, PaymentEvents.ProcessingFailed> paymentRecord = 
                    createConsumerRecord("payment.v1", paymentFailedEvent);

            // act - 보상 트랜잭션 실행
            couponConsumer.handlePaymentProcessingFailed(paymentRecord, acknowledgment);
            waitForAsyncProcessing(1000);

            // stockConsumer를 위해 별도의 PaymentProcessingFailed 이벤트 생성 (다른 eventId 사용)
            PaymentEvents.ProcessingFailed paymentFailedEventForStock = new PaymentEvents.ProcessingFailed(
                    orderId,
                    couponProcessedEvent,
                    "결제 처리 실패"
            );
            ConsumerRecord<String, PaymentEvents.ProcessingFailed> paymentRecordForStock = 
                    createConsumerRecord("payment.v1", paymentFailedEventForStock);

            stockConsumer.handlePaymentProcessingFailed(paymentRecordForStock, acknowledgment);
            waitForAsyncProcessing(1000);

            // orderConsumer를 위해 별도의 PaymentProcessingFailed 이벤트 생성 (다른 eventId 사용)
            PaymentEvents.ProcessingFailed paymentFailedEventForOrder = new PaymentEvents.ProcessingFailed(
                    orderId,
                    couponProcessedEvent,
                    "결제 처리 실패"
            );
            ConsumerRecord<String, PaymentEvents.ProcessingFailed> paymentRecordForOrder = 
                    createConsumerRecord("payment.v1", paymentFailedEventForOrder);

            orderConsumer.handlePaymentProcessingFailed(paymentRecordForOrder, acknowledgment);
            waitForAsyncProcessing(1000);

            // assert - 쿠폰이 원복되었는지 확인
            Coupon couponAfter = couponRepository.findById(testCouponId)
                    .orElseThrow(() -> new RuntimeException("Coupon을 찾을 수 없습니다"));
            assertFalse(couponAfter.getIsUsed(), "쿠폰이 원복되어야 함");
            assertNull(couponAfter.getOrderId(), "쿠폰의 orderId가 null이어야 함");

            // assert - 재고가 원복되었는지 확인
            Stock stockAfter = stockRepository.findByProductId(testProductId)
                    .orElseThrow(() -> new RuntimeException("Stock을 찾을 수 없습니다"));
            assertEquals(initialStockQuantity, stockAfter.getQuantity(),
                    "재고가 원복되어야 함");

            // assert - 주문 상태가 PAYMENT_FAILED로 변경되었는지 확인
            Order failedOrder = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order를 찾을 수 없습니다"));
            assertEquals(OrderStatus.PAYMENT_FAILED, failedOrder.getOrderStatus(),
                    "주문 상태가 PAYMENT_FAILED로 변경되어야 함");
        }
    }

    private void waitForAsyncProcessing() throws InterruptedException {
        waitForAsyncProcessing(500);
    }

    private void waitForAsyncProcessing(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }
}

