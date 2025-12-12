package com.loopers.domain.stock.event;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandStatus;
import com.loopers.domain.coupon.event.CouponEvents;
import com.loopers.domain.order.event.OrderEvents;
import com.loopers.domain.payment.event.PaymentEvents;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductStatus;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.stock.Stock;
import com.loopers.domain.stock.StockRepository;
import com.loopers.domain.stock.StockService;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserRepository;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.interfaces.api.order.OrderDto;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StockEventListener 통합 테스트")
@SpringBootTest
class StockEventListenerIntegrationTest {

    @Autowired
    private StockEventListener stockEventListener;

    @MockitoBean
    private StockEventPublisher stockEventPublisher;

    @Autowired
    private StockService stockService;

    @Autowired
    private StockRepository stockRepository;

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

    @Autowired
    private OrderService orderService;

    private Long testUserId;
    private Long testProductId1;
    private Long testProductId2;
    private final Long initialStockQuantity1 = 100L;
    private final Long initialStockQuantity2 = 50L;

    private final String testLoginId = "test34";

    @BeforeEach
    void setUp() {
        // 테스트용 User 생성
        UserInfo userInfo = UserInfo.builder()
                .loginId(testLoginId)
                .email("test@test.com")
                .birthday("1990-01-01")
                .gender(Gender.MALE)
                .build();
        userFacade.saveUser(userInfo);

        testUserId = userRepository.findByLoginId(testLoginId)
                .orElseThrow(() -> new RuntimeException("User를 찾을 수 없습니다"))
                .getId();

        // 테스트용 Brand 생성
        Brand brand = Brand.builder()
                .name("Test Brand")
                .description("Test Description")
                .status(BrandStatus.ON_SALE)
                .isVisible(true)
                .isSellable(true)
                .build();
        Brand savedBrand = brandJpaRepository.save(brand);

        // 테스트용 Product 1 생성
        Product product1 = Product.builder()
                .name("Test Product 1")
                .description("Test Description")
                .price(BigDecimal.valueOf(10000))
                .status(ProductStatus.ON_SALE)
                .isVisible(true)
                .isSellable(true)
                .brandId(savedBrand.getId())
                .build();
        Product savedProduct1 = productRepository.save(product1)
                .orElseThrow(() -> new RuntimeException("Product 저장 실패"));
        testProductId1 = savedProduct1.getId();

        // 테스트용 Product 2 생성
        Product product2 = Product.builder()
                .name("Test Product 2")
                .description("Test Description")
                .price(BigDecimal.valueOf(20000))
                .status(ProductStatus.ON_SALE)
                .isVisible(true)
                .isSellable(true)
                .brandId(savedBrand.getId())
                .build();
        Product savedProduct2 = productRepository.save(product2)
                .orElseThrow(() -> new RuntimeException("Product 저장 실패"));
        testProductId2 = savedProduct2.getId();

        // Stock 생성 및 초기 재고 설정
        Stock stock1 = Stock.builder()
                .productId(testProductId1)
                .quantity(0L)
                .build();
        stockService.saveStock(stock1)
                .orElseThrow(() -> new RuntimeException("Stock 저장 실패"));
        stockService.increaseQuantity(testProductId1, initialStockQuantity1);

        Stock stock2 = Stock.builder()
                .productId(testProductId2)
                .quantity(0L)
                .build();
        stockService.saveStock(stock2)
                .orElseThrow(() -> new RuntimeException("Stock 저장 실패"));
        stockService.increaseQuantity(testProductId2, initialStockQuantity2);
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("handleOrderCreated 테스트")
    @Nested
    class HandleOrderCreatedTest {

        @DisplayName("성공 케이스: 재고 차감 성공 시 재고가 정확히 차감됨")
        @Test
        void handleOrderCreated_withValidEvent_decreasesStock() throws InterruptedException {
            // arrange
            // 먼저 주문 생성 (이벤트 리스너가 주문을 찾을 수 있도록)
            Order order = Order.builder()
                    .userId(testUserId)
                    .discountAmount(BigDecimal.ZERO)
                    .shippingFee(BigDecimal.ZERO)
                    .build();
            order.addOrderItem(testProductId1, "Test Product 1", BigDecimal.valueOf(10000), 5);
            order.addOrderItem(testProductId2, "Test Product 2", BigDecimal.valueOf(20000), 10);
            Order savedOrder = orderService.saveOrder(order);
            Long orderId = savedOrder.getId();

            OrderDto.CreateOrderRequest request = OrderDto.CreateOrderRequest.builder()
                    .items(List.of(
                            OrderDto.OrderItemRequest.builder()
                                    .productId(testProductId1)
                                    .quantity(5)
                                    .build(),
                            OrderDto.OrderItemRequest.builder()
                                    .productId(testProductId2)
                                    .quantity(10)
                                    .build()
                    ))
                    .couponIds(List.of())
                    .build();

            OrderEvents.Created event = new OrderEvents.Created(
                    testUserId,
                    orderId, // 실제 생성된 주문 ID 사용
                    BigDecimal.valueOf(250000), // totalPrice
                    request
            );

            // act
            stockEventListener.handleOrderCreated(event);
            
            // 비동기 처리 대기 (더 긴 시간 사용)
            waitForAsyncProcessing(1000);

            // assert - 재고가 차감되었는지 확인 (재시도 로직 사용)
            Stock stock1 = waitForStockToDecrease(testProductId1, initialStockQuantity1 - 5L, 5);
            Stock stock2 = waitForStockToDecrease(testProductId2, initialStockQuantity2 - 10L, 5);

            assertNotNull(stock1, "Stock1을 찾을 수 없습니다");
            assertNotNull(stock2, "Stock2를 찾을 수 없습니다");
            assertEquals(initialStockQuantity1 - 5L, stock1.getQuantity(),
                    "Product1의 재고가 5개 차감되어야 함");
            assertEquals(initialStockQuantity2 - 10L, stock2.getQuantity(),
                    "Product2의 재고가 10개 차감되어야 함");
        }

        @DisplayName("실패 케이스: 재고 부족 시 예외 발생하고 재고 차감 안 됨")
        @Test
        void handleOrderCreated_withInsufficientStock_throwsException() throws InterruptedException {
            // arrange
            OrderDto.CreateOrderRequest request = OrderDto.CreateOrderRequest.builder()
                    .items(List.of(
                            OrderDto.OrderItemRequest.builder()
                                    .productId(testProductId1)
                                    .quantity(200) // 재고보다 많은 수량
                                    .build()
                    ))
                    .couponIds(List.of())
                    .build();

            OrderEvents.Created event = new OrderEvents.Created(
                    testUserId,
                    100L, // orderId
                    BigDecimal.valueOf(2000000), // totalPrice
                    request
            );

            // act
            stockEventListener.handleOrderCreated(event);
            waitForAsyncProcessing();

            // assert - 재고가 차감되지 않아야 함
            Stock stock1 = stockRepository.findByProductId(testProductId1)
                    .orElseThrow(() -> new RuntimeException("Stock을 찾을 수 없습니다"));

            assertEquals(initialStockQuantity1, stock1.getQuantity(),
                    "재고 부족으로 재고가 차감되지 않아야 함");
        }
    }

    @DisplayName("handleCouponProcessingFailed 테스트")
    @Nested
    class HandleCouponProcessingFailedTest {

        @DisplayName("성공 케이스: 재고 원복 성공")
        @Test
        void handleCouponProcessingFailed_withValidEvent_compensatesStock() throws InterruptedException {
            // arrange
            // 먼저 재고 차감
            stockService.decreaseQuantity(testProductId1, 5L);
            stockService.decreaseQuantity(testProductId2, 10L);

            // 재고 차감 확인
            Stock stock1Before = stockRepository.findByProductId(testProductId1)
                    .orElseThrow(() -> new RuntimeException("Stock을 찾을 수 없습니다"));
            Stock stock2Before = stockRepository.findByProductId(testProductId2)
                    .orElseThrow(() -> new RuntimeException("Stock을 찾을 수 없습니다"));
            assertEquals(initialStockQuantity1 - 5L, stock1Before.getQuantity());
            assertEquals(initialStockQuantity2 - 10L, stock2Before.getQuantity());

            // CouponEvents.ProcessingFailed 이벤트 생성
            OrderDto.CreateOrderRequest request = OrderDto.CreateOrderRequest.builder()
                    .items(List.of(
                            OrderDto.OrderItemRequest.builder()
                                    .productId(testProductId1)
                                    .quantity(5)
                                    .build(),
                            OrderDto.OrderItemRequest.builder()
                                    .productId(testProductId2)
                                    .quantity(10)
                                    .build()
                    ))
                    .couponIds(List.of())
                    .build();

            OrderEvents.Created orderCreatedEvent = new OrderEvents.Created(
                    testUserId,
                    100L,
                    BigDecimal.valueOf(250000),
                    request
            );

            StockEvents.Processed stockProcessedEvent = new StockEvents.Processed(
                    100L,
                    List.of(
                            new StockEvents.OrderItemInfo(testProductId1, 5),
                            new StockEvents.OrderItemInfo(testProductId2, 10)
                    ),
                    orderCreatedEvent
            );

            CouponEvents.ProcessingFailed event = new CouponEvents.ProcessingFailed(
                    100L,
                    stockProcessedEvent,
                    "쿠폰 사용 실패"
            );

            // act
            stockEventListener.handleCouponProcessingFailed(event);
            waitForAsyncProcessing();

            // assert - 재고가 원복되어야 함
            Stock stock1After = stockRepository.findByProductId(testProductId1)
                    .orElseThrow(() -> new RuntimeException("Stock을 찾을 수 없습니다"));
            Stock stock2After = stockRepository.findByProductId(testProductId2)
                    .orElseThrow(() -> new RuntimeException("Stock을 찾을 수 없습니다"));

            assertEquals(initialStockQuantity1, stock1After.getQuantity(),
                    "Product1의 재고가 원복되어야 함");
            assertEquals(initialStockQuantity2, stock2After.getQuantity(),
                    "Product2의 재고가 원복되어야 함");
        }
    }

    @DisplayName("handlePaymentProcessingFailed 테스트")
    @Nested
    class HandlePaymentProcessingFailedTest {

        @DisplayName("성공 케이스: 재고 원복 성공")
        @Test
        void handlePaymentProcessingFailed_withValidEvent_compensatesStock() throws InterruptedException {
            // arrange
            // 먼저 재고 차감
            stockService.decreaseQuantity(testProductId1, 5L);
            stockService.decreaseQuantity(testProductId2, 10L);

            // 재고 차감 확인
            Stock stock1Before = stockRepository.findByProductId(testProductId1)
                    .orElseThrow(() -> new RuntimeException("Stock을 찾을 수 없습니다"));
            Stock stock2Before = stockRepository.findByProductId(testProductId2)
                    .orElseThrow(() -> new RuntimeException("Stock을 찾을 수 없습니다"));
            assertEquals(initialStockQuantity1 - 5L, stock1Before.getQuantity());
            assertEquals(initialStockQuantity2 - 10L, stock2Before.getQuantity());

            // PaymentEvents.ProcessingFailed 이벤트 생성
            OrderDto.CreateOrderRequest request = OrderDto.CreateOrderRequest.builder()
                    .items(List.of(
                            OrderDto.OrderItemRequest.builder()
                                    .productId(testProductId1)
                                    .quantity(5)
                                    .build(),
                            OrderDto.OrderItemRequest.builder()
                                    .productId(testProductId2)
                                    .quantity(10)
                                    .build()
                    ))
                    .couponIds(List.of())
                    .build();

            OrderEvents.Created orderCreatedEvent = new OrderEvents.Created(
                    testUserId,
                    100L,
                    BigDecimal.valueOf(250000),
                    request
            );

            StockEvents.Processed stockProcessedEvent = new StockEvents.Processed(
                    100L,
                    List.of(
                            new StockEvents.OrderItemInfo(testProductId1, 5),
                            new StockEvents.OrderItemInfo(testProductId2, 10)
                    ),
                    orderCreatedEvent
            );

            com.loopers.domain.coupon.event.CouponEvents.Processed couponProcessedEvent =
                    new com.loopers.domain.coupon.event.CouponEvents.Processed(
                            100L,
                            testUserId,
                            BigDecimal.valueOf(5000),
                            stockProcessedEvent
                    );

            PaymentEvents.ProcessingFailed event = new PaymentEvents.ProcessingFailed(
                    100L,
                    couponProcessedEvent,
                    "결제 처리 실패"
            );

            // act
            stockEventListener.handlePaymentProcessingFailed(event);
            waitForAsyncProcessing();

            // assert - 재고가 원복되어야 함
            Stock stock1After = stockRepository.findByProductId(testProductId1)
                    .orElseThrow(() -> new RuntimeException("Stock을 찾을 수 없습니다"));
            Stock stock2After = stockRepository.findByProductId(testProductId2)
                    .orElseThrow(() -> new RuntimeException("Stock을 찾을 수 없습니다"));

            assertEquals(initialStockQuantity1, stock1After.getQuantity(),
                    "Product1의 재고가 원복되어야 함");
            assertEquals(initialStockQuantity2, stock2After.getQuantity(),
                    "Product2의 재고가 원복되어야 함");
        }
    }

    /**
     * 비동기 이벤트 핸들러 완료 대기
     */
    private void waitForAsyncProcessing() throws InterruptedException {
        waitForAsyncProcessing(500);
    }
    
    /**
     * 비동기 이벤트 핸들러 완료 대기 (지정된 시간)
     */
    private void waitForAsyncProcessing(long millis) throws InterruptedException {
        Thread.sleep(millis); // @Async 메서드 완료 대기
    }
    
    /**
     * 재고가 특정 값이 될 때까지 대기 (재시도 로직 포함)
     */
    private Stock waitForStockToDecrease(Long productId, Long expectedQuantity, int maxRetries) throws InterruptedException {
        Stock stock = null;
        for (int i = 0; i < maxRetries; i++) {
            stock = stockRepository.findByProductId(productId)
                    .orElseThrow(() -> new RuntimeException("Stock을 찾을 수 없습니다"));
            if (stock.getQuantity().equals(expectedQuantity)) {
                return stock;
            }
            Thread.sleep(200); // 200ms 대기 후 재시도
        }
        // 마지막으로 한 번 더 확인
        if (stock == null) {
            stock = stockRepository.findByProductId(productId)
                    .orElseThrow(() -> new RuntimeException("Stock을 찾을 수 없습니다"));
        }
        return stock;
    }
}
