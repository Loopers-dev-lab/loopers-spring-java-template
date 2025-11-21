package com.loopers.application.order;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponRepository;
import com.loopers.domain.coupon.CouponType;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.stock.Stock;
import com.loopers.domain.stock.StockRepository;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandStatus;
import com.loopers.domain.product.ProductStatus;
import com.loopers.interfaces.api.order.OrderDto;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OrderFacade 통합 테스트")
@SpringBootTest
class OrderFacadeTest {

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private UserFacade userFacade;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandJpaRepository brandJpaRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private com.loopers.domain.stock.StockService stockService;

    @Autowired
    private PointService pointService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private com.loopers.domain.order.OrderService orderService;

    @Autowired
    private com.loopers.domain.coupon.CouponService couponService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private User testUser;
    private Product testProduct;
    private Long testProductId;
    
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

        testUser = userRepository.findByLoginId(testLoginId)
                .orElseThrow(() -> new RuntimeException("User를 찾을 수 없습니다"));

        // 테스트용 Brand 생성
        Brand brand = Brand.builder()
                .name("Test Brand")
                .description("Test Description")
                .status(BrandStatus.ON_SALE)
                .isVisible(true)
                .isSellable(true)
                .build();
        Brand savedBrand = brandJpaRepository.save(brand);

        // 테스트용 Product 생성
        Product product = Product.builder()
                .name("Test Product")
                .description("Test Description")
                .price(BigDecimal.valueOf(10000))
                .status(ProductStatus.ON_SALE)
                .isVisible(true)
                .isSellable(true)
                .brand(savedBrand)
                .build();
        Product savedProduct = productRepository.save(product)
                .orElseThrow(() -> new RuntimeException("Product 저장 실패"));
        testProduct = savedProduct;
        testProductId = savedProduct.getId();

        // 테스트용 Stock 생성 (재고 100개로 설정)
        // Product 생성 시 Stock이 자동으로 생성되므로, 재고를 100개로 증가
        Stock stock = stockRepository.findByProductId(testProductId)
                .orElseThrow(() -> new RuntimeException("Stock을 찾을 수 없습니다"));
        // 현재 재고를 확인하고 100개가 되도록 조정
        long currentQuantity = stock.getQuantity();
        if (currentQuantity < 100L) {
            stockService.increaseQuantity(testProductId, 100L - currentQuantity);
        }

        // 테스트용 포인트 충전 (100000원)
        pointService.charge(testLoginId, BigDecimal.valueOf(100000));
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("createOrder 테스트")
    @Nested
    class CreateOrderTest {

        @DisplayName("성공 케이스: 모든 조건을 만족하는 주문 생성 성공")
        @Test
        void createOrder_withValidRequest_Success() {
            // arrange
            List<OrderDto.OrderItemRequest> items = List.of(
                    OrderDto.OrderItemRequest.builder()
                            .productId(testProductId)
                            .quantity(2)
                            .build()
            );
            OrderDto.CreateOrderRequest request = OrderDto.CreateOrderRequest.builder()
                    .items(items)
                    .couponIds(new ArrayList<>())
                    .build();

            BigDecimal expectedTotalPrice = BigDecimal.valueOf(10000).multiply(BigDecimal.valueOf(2)); // 20000
            BigDecimal expectedFinalAmount = expectedTotalPrice; // 할인 없음

            // act
            OrderInfo orderInfo = orderFacade.createOrder(testLoginId, request);

            // assert
            assertNotNull(orderInfo);
            assertAll(
                    () -> assertNotNull(orderInfo.id(), "주문 ID는 null이 아니어야 함"),
                    () -> assertEquals(0, expectedTotalPrice.compareTo(orderInfo.totalPrice()), "총 가격이 일치해야 함"),
                    () -> assertEquals(0, expectedFinalAmount.compareTo(orderInfo.finalAmount()), "최종 금액이 일치해야 함"),
                    () -> assertEquals(OrderStatus.CONFIRMED, orderInfo.orderStatus(), "주문 상태는 CONFIRMED여야 함"),
                    () -> assertEquals(testLoginId, orderInfo.userLoginId(), "사용자 로그인 ID가 일치해야 함"),
                    () -> assertEquals(1, orderInfo.orderItems().size(), "주문 상품 수가 일치해야 함")
            );

            // 재고가 차감되었는지 확인
            Stock stock = stockRepository.findByProductId(testProductId)
                    .orElseThrow(() -> new RuntimeException("Stock을 찾을 수 없습니다"));
            assertEquals(98L, stock.getQuantity(), "재고가 2개 차감되어야 함 (100 - 2 = 98)");

            // 포인트가 차감되었는지 확인
            Point point = pointService.findByUserLoginId(testLoginId)
                    .orElseThrow(() -> new RuntimeException("Point를 찾을 수 없습니다"));
            BigDecimal expectedPoint = BigDecimal.valueOf(100000).subtract(expectedFinalAmount);
            assertEquals(0, expectedPoint.compareTo(point.getAmount()), "포인트가 차감되어야 함");
        }

        @DisplayName("성공 케이스: 쿠폰 적용하여 주문 생성 성공")
        @Test
        void createOrder_withCoupon_Success() {
            // arrange
            // 쿠폰 생성
            Coupon coupon = Coupon.builder()
                    .couponType(CouponType.FIXED_AMOUNT)
                    .discountValue(BigDecimal.valueOf(5000))
                    .user(testUser)
                    .build();
            Coupon savedCoupon = couponRepository.save(coupon)
                    .orElseThrow(() -> new RuntimeException("Coupon 저장 실패"));

            List<OrderDto.OrderItemRequest> items = List.of(
                    OrderDto.OrderItemRequest.builder()
                            .productId(testProductId)
                            .quantity(2)
                            .build()
            );
            OrderDto.CreateOrderRequest request = OrderDto.CreateOrderRequest.builder()
                    .items(items)
                    .couponIds(List.of(savedCoupon.getId()))
                    .build();

            BigDecimal expectedTotalPrice = BigDecimal.valueOf(10000).multiply(BigDecimal.valueOf(2)); // 20000
            BigDecimal expectedDiscount = BigDecimal.valueOf(5000);
            BigDecimal expectedFinalAmount = expectedTotalPrice.subtract(expectedDiscount); // 15000

            // act
            OrderInfo orderInfo = orderFacade.createOrder(testLoginId, request);

            // assert
            assertNotNull(orderInfo);
            assertAll(
                    () -> assertEquals(0, expectedTotalPrice.compareTo(orderInfo.totalPrice()), "총 가격이 일치해야 함"),
                    () -> assertEquals(0, expectedDiscount.compareTo(orderInfo.discountAmount()), "할인 금액이 일치해야 함"),
                    () -> assertEquals(0, expectedFinalAmount.compareTo(orderInfo.finalAmount()), "최종 금액이 일치해야 함")
            );

            // 쿠폰이 사용되었는지 확인
            Coupon usedCoupon = couponRepository.findById(savedCoupon.getId())
                    .orElseThrow(() -> new RuntimeException("Coupon을 찾을 수 없습니다"));
            assertTrue(usedCoupon.getIsUsed(), "쿠폰은 사용된 상태여야 함");
            assertNotNull(usedCoupon.getOrder(), "쿠폰의 order는 null이 아니어야 함");
        }

        @DisplayName("실패 케이스: 존재하지 않는 User로 주문 생성 시 NOT_FOUND 예외 발생")
        @Test
        void createOrder_withNonExistentUser_NotFound() {
            // arrange
            List<OrderDto.OrderItemRequest> items = List.of(
                    OrderDto.OrderItemRequest.builder()
                            .productId(testProductId)
                            .quantity(2)
                            .build()
            );
            OrderDto.CreateOrderRequest request = OrderDto.CreateOrderRequest.builder()
                    .items(items)
                    .couponIds(new ArrayList<>())
                    .build();

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () ->
                    orderFacade.createOrder("nonexistent", request)
            );

            assertEquals(ErrorType.NOT_FOUND, exception.getErrorType());
            assertTrue(exception.getCustomMessage().contains("[loginId = nonexistent] User를 찾을 수 없습니다"));
        }

        @DisplayName("실패 케이스: 존재하지 않는 Product로 주문 생성 시 NOT_FOUND 예외 발생")
        @Test
        void createOrder_withNonExistentProduct_NotFound() {
            // arrange
            List<OrderDto.OrderItemRequest> items = List.of(
                    OrderDto.OrderItemRequest.builder()
                            .productId(99999L)
                            .quantity(2)
                            .build()
            );
            OrderDto.CreateOrderRequest request = OrderDto.CreateOrderRequest.builder()
                    .items(items)
                    .couponIds(new ArrayList<>())
                    .build();

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () ->
                    orderFacade.createOrder(testLoginId, request)
            );

            assertEquals(ErrorType.NOT_FOUND, exception.getErrorType());
            assertTrue(exception.getCustomMessage().contains("[productId = 99999] Product를 찾을 수 없습니다"));
        }

        @DisplayName("실패 케이스: 재고 부족 시 BAD_REQUEST 예외 발생")
        @Test
        void createOrder_withInsufficientStock_BadRequest() {
            // arrange
            // 재고를 5개로 설정 (현재 재고를 확인하고 5개가 되도록 조정)
            Stock stock = stockRepository.findByProductId(testProductId)
                    .orElseThrow(() -> new RuntimeException("Stock을 찾을 수 없습니다"));
            long currentQuantity = stock.getQuantity();
            if (currentQuantity > 5L) {
                // 재고를 5개로 만들기 위해 차감
                stockService.decreaseQuantity(testProductId, currentQuantity - 5L);
            } else if (currentQuantity < 5L) {
                // 재고를 5개로 만들기 위해 증가
                stockService.increaseQuantity(testProductId, 5L - currentQuantity);
            }

            List<OrderDto.OrderItemRequest> items = List.of(
                    OrderDto.OrderItemRequest.builder()
                            .productId(testProductId)
                            .quantity(10) // 재고보다 많은 수량
                            .build()
            );
            OrderDto.CreateOrderRequest request = OrderDto.CreateOrderRequest.builder()
                    .items(items)
                    .couponIds(new ArrayList<>())
                    .build();

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () ->
                    orderFacade.createOrder(testLoginId, request)
            );

            assertEquals(ErrorType.BAD_REQUEST, exception.getErrorType());
            assertTrue(exception.getCustomMessage().contains("재고가 부족"));
        }

        @DisplayName("실패 케이스: 포인트 부족 시 BAD_REQUEST 예외 발생")
        @Test
        void createOrder_withInsufficientPoint_BadRequest() {
            // arrange
            // 포인트를 1000원으로 조정 (기존 포인트를 모두 차감 후 1000원 충전)
            Point currentPoint = pointService.findByUserLoginId(testLoginId)
                    .orElseThrow(() -> new RuntimeException("Point를 찾을 수 없습니다"));
            BigDecimal currentAmount = currentPoint.getAmount();
            if (currentAmount.compareTo(BigDecimal.valueOf(1000)) > 0) {
                pointService.deduct(testLoginId, currentAmount.subtract(BigDecimal.valueOf(1000)));
            } else if (currentAmount.compareTo(BigDecimal.valueOf(1000)) < 0) {
                pointService.charge(testLoginId, BigDecimal.valueOf(1000).subtract(currentAmount));
            }

            List<OrderDto.OrderItemRequest> items = List.of(
                    OrderDto.OrderItemRequest.builder()
                            .productId(testProductId)
                            .quantity(10) // 100000원 (포인트보다 많은 금액)
                            .build()
            );
            OrderDto.CreateOrderRequest request = OrderDto.CreateOrderRequest.builder()
                    .items(items)
                    .couponIds(new ArrayList<>())
                    .build();

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () ->
                    orderFacade.createOrder(testLoginId, request)
            );

            assertEquals(ErrorType.BAD_REQUEST, exception.getErrorType());
            assertTrue(exception.getCustomMessage().contains("포인트가 부족"));
        }

        @DisplayName("실패 케이스: 쿠폰 사용 불가 시 BAD_REQUEST 예외 발생")
        @Test
        void createOrder_withUnavailableCoupon_BadRequest() {
            // arrange
            // 쿠폰 생성
            Coupon coupon = Coupon.builder()
                    .couponType(CouponType.FIXED_AMOUNT)
                    .discountValue(BigDecimal.valueOf(5000))
                    .user(testUser)
                    .build();
            Coupon savedCoupon = couponRepository.save(coupon)
                    .orElseThrow(() -> new RuntimeException("Coupon 저장 실패"));

            // 쿠폰을 먼저 사용 (실제 Order를 생성하여 쿠폰 사용)
            Order firstOrder = Order.builder()
                    .discountAmount(BigDecimal.ZERO)
                    .shippingFee(BigDecimal.ZERO)
                    .user(testUser)
                    .build();
            firstOrder.addOrderItem(testProduct, 1);
            
            // Order 저장
            Order savedFirstOrder = orderService.saveOrder(firstOrder)
                    .orElseThrow(() -> new RuntimeException("Order 저장 실패"));

            // CouponService를 사용하여 쿠폰 사용 (guard() 검증을 통과하도록)
            couponService.useCoupon(savedFirstOrder, savedCoupon.getId());

            // 이미 사용된 쿠폰으로 다시 주문 생성 시도
            List<OrderDto.OrderItemRequest> items = List.of(
                    OrderDto.OrderItemRequest.builder()
                            .productId(testProductId)
                            .quantity(2)
                            .build()
            );
            OrderDto.CreateOrderRequest request = OrderDto.CreateOrderRequest.builder()
                    .items(items)
                    .couponIds(List.of(savedCoupon.getId()))
                    .build();

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () ->
                    orderFacade.createOrder(testLoginId, request)
            );

            assertEquals(ErrorType.BAD_REQUEST, exception.getErrorType());
            assertTrue(exception.getCustomMessage().contains("쿠폰을 사용할 수 없습니다"));
        }
    }

    @DisplayName("동시성 테스트")
    @Nested
    class ConcurrencyTest {

        @DisplayName("여러 주문이 동시에 들어와도 재고/포인트/쿠폰이 일관되게 처리된다")
        @Test
        void createOrder_concurrentRequests_consistencyMaintained() throws InterruptedException {
            // arrange
            int initialStock = 10;
            int couponCount = 5;
            Stock stock = stockRepository.findByProductId(testProductId)
                    .orElseThrow(() -> new RuntimeException("Stock을 찾을 수 없습니다"));
            long currentQuantity = stock.getQuantity();
            if (currentQuantity > initialStock) {
                stockService.decreaseQuantity(testProductId, currentQuantity - initialStock);
            } else if (currentQuantity < initialStock) {
                stockService.increaseQuantity(testProductId, initialStock - currentQuantity);
            }

            List<OrderDto.OrderItemRequest> items = List.of(
                    OrderDto.OrderItemRequest.builder()
                            .productId(testProductId)
                            .quantity(1)
                            .build()
            );
            OrderDto.CreateOrderRequest request = OrderDto.CreateOrderRequest.builder()
                    .items(items)
                    .couponIds(new ArrayList<>())
                    .build();

            List<Long> couponIds = new ArrayList<>();
            for (int i = 0; i < couponCount; i++) {
                Coupon coupon = Coupon.builder()
                        .couponType(CouponType.FIXED_AMOUNT)
                        .discountValue(BigDecimal.valueOf(5000))
                        .user(testUser)
                        .build();
                Long couponId = couponRepository.save(coupon)
                        .orElseThrow(() -> new RuntimeException("Coupon 저장 실패"))
                        .getId();
                couponIds.add(couponId);
            }
            ConcurrentLinkedQueue<Long> couponQueue = new ConcurrentLinkedQueue<>(couponIds);

            int threadCount = 20;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch ready = new CountDownLatch(threadCount);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threadCount);

            List<OrderInfo> successOrders = Collections.synchronizedList(new ArrayList<>());
            List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        Long couponId = couponQueue.poll();
                        OrderDto.CreateOrderRequest actualRequest = couponId == null
                                ? request
                                : OrderDto.CreateOrderRequest.builder()
                                    .items(items)
                                    .couponIds(List.of(couponId))
                                    .build();
                        OrderInfo orderInfo = orderFacade.createOrder(testLoginId, actualRequest);
                        successOrders.add(orderInfo);
                    } catch (Throwable t) {
                        failures.add(t);
                    } finally {
                        done.countDown();
                    }
                });
            }

            ready.await();
            start.countDown();
            done.await();
            executor.shutdownNow();

            // assert
            assertEquals(initialStock, successOrders.size(), "재고 수만큼만 주문이 성공해야 함");
            assertTrue(failures.size() >= threadCount - initialStock, "초과 주문은 실패해야 함");
            failures.forEach(failure -> {
                assertTrue(failure instanceof CoreException, "실패는 CoreException이어야 함");
                CoreException exception = (CoreException) failure;
                assertEquals(ErrorType.BAD_REQUEST, exception.getErrorType(), "실패 타입은 BAD_REQUEST여야 함");
                assertTrue(exception.getCustomMessage().contains("재고가 부족"), "실패 메시지는 재고 부족이어야 함");
            });

            Stock finalStock = stockRepository.findByProductId(testProductId)
                    .orElseThrow(() -> new RuntimeException("Stock을 찾을 수 없습니다"));
            assertEquals(0L, finalStock.getQuantity(), "재고는 0이어야 함");

            BigDecimal productPrice = testProduct.getPrice();
            BigDecimal couponDiscount = BigDecimal.valueOf(5000);
            
            // 쿠폰 검증은 트랜잭션 내에서 수행
            long usedCouponCount = verifyCoupons(couponIds, initialStock, couponDiscount);

            long nonCouponSuccessCount = initialStock - usedCouponCount;
            BigDecimal totalCouponOrders = productPrice.subtract(couponDiscount)
                    .multiply(BigDecimal.valueOf(usedCouponCount));
            BigDecimal totalNonCouponOrders = productPrice
                    .multiply(BigDecimal.valueOf(nonCouponSuccessCount));
            BigDecimal expectedPoint = BigDecimal.valueOf(100000)
                    .subtract(totalCouponOrders.add(totalNonCouponOrders));
            Point point = pointService.findByUserLoginId(testLoginId)
                    .orElseThrow(() -> new RuntimeException("Point를 찾을 수 없습니다"));
            assertEquals(0, expectedPoint.compareTo(point.getAmount()), "사용된 포인트가 정확해야 함");

            successOrders.forEach(orderInfo -> {
                assertEquals(OrderStatus.CONFIRMED, orderInfo.orderStatus(), "성공 주문 상태 검증");
                assertEquals(1, orderInfo.orderItems().size(), "주문 상품 수는 1개");
                if (orderInfo.discountAmount().compareTo(BigDecimal.ZERO) > 0) {
                    assertEquals(0, couponDiscount.compareTo(orderInfo.discountAmount()), "쿠폰 할인 금액 일치");
                } else {
                    assertEquals(BigDecimal.ZERO, orderInfo.discountAmount(), "쿠폰이 없으면 할인 없음");
                }
                BigDecimal expectedFinalAmount = productPrice.subtract(orderInfo.discountAmount());
                assertEquals(0, expectedFinalAmount.compareTo(orderInfo.finalAmount()), "최종 금액 검증");
            });
        }

        private long verifyCoupons(List<Long> couponIds, int initialStock, BigDecimal couponDiscount) {
            Long result = transactionTemplate.execute(status -> {
                long usedCouponCount = couponIds.stream()
                        .filter(id -> id != null)
                        .map(id -> couponRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Coupon을 찾을 수 없습니다")))
                        .filter(Coupon::getIsUsed)
                        .peek(coupon -> {
                            assertNotNull(coupon.getOrder(), "쿠폰은 주문과 연결되어야 함");
                        })
                        .count();
                assertTrue(usedCouponCount <= initialStock, "사용된 쿠폰 수는 성공 주문 수를 초과할 수 없음");

                couponIds.stream()
                        .filter(id -> id != null)
                        .forEach(id -> {
                            Coupon coupon = couponRepository.findById(id)
                                    .orElseThrow(() -> new RuntimeException("Coupon을 찾을 수 없습니다"));
                            if (coupon.getIsUsed()) {
                                Order order = coupon.getOrder();
                                assertEquals(couponDiscount.compareTo(order.getDiscountAmount()), 0, "쿠폰 사용 주문 할인 금액 검증");
                            }
                        });
                
                return usedCouponCount;
            });
            return result != null ? result : 0L;
        }
    }
}

