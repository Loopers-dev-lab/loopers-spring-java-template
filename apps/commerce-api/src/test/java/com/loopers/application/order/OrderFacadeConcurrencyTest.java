package com.loopers.application.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.loopers.application.point.PointCommand;
import com.loopers.application.point.PointFacade;
import com.loopers.application.point.PointInfo;
import com.loopers.application.user.UserCommand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.coupon.Discount;
import com.loopers.domain.product.LikeCount;
import com.loopers.domain.product.Price;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.Stock;
import com.loopers.domain.user.UserService;
import com.loopers.utils.DatabaseCleanUp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
class OrderFacadeConcurrencyTest {

    @Autowired
    private OrderLockFacade orderLockFacade;

    @Autowired
    private UserService userService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private PointFacade pointFacade;

    @Autowired
    private CouponService couponService;

    @MockitoSpyBean
    private BrandService brandService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private static final OrderCommand.DeliveryCommand DEFAULT_DELIVERY_COMMAND = new OrderCommand.DeliveryCommand(
            "구매자",
            "010-1234-5678",
            "서울",
            "상세주소"
    );

    private String loginId;
    private List<Long> productIds;
    private int initialPoint;

    @BeforeEach
    void setUp() {
        loginId = "testUser";
        UserCommand.SignupCommand signupCommand = new UserCommand.SignupCommand(
                loginId,
                "test@test.com",
                "2000-01-01",
                "M"
        );
        userService.signup(signupCommand);

        initialPoint = 1000000;
        PointCommand.ChargeCommand chargeCommand = new PointCommand.ChargeCommand(loginId, initialPoint);
        pointFacade.chargePoint(chargeCommand);

        productIds = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Product product = Product.createProduct(
                    "상품" + i,
                    1L,
                    Price.createPrice(1000 * i),
                    LikeCount.createLikeCount(0),
                    Stock.createStock(100)
            );
            productRepository.saveProduct(product);
            productIds.add(product.getId());
        }
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("동일한 유저가 서로 다른 주문을 동시에 수행할 경우, 포인트가 정상적으로 차감되어야 한다.")
    @Test
    void shouldDeductPointCorrectly_whenSameUserCreatesMultipleOrdersConcurrently() throws Exception {
        // arrange
        int orderCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(orderCount);
        CountDownLatch latch = new CountDownLatch(orderCount);
        List<Future<OrderInfo>> futures = new ArrayList<>();

        int[] quantities = {1, 2, 3, 4, 5};
        int expectedTotalDeduction = 0;

        // act
        for (int i = 0; i < orderCount; i++) {
            final int quantity = quantities[i];
            final Long productId = productIds.get(i);
            expectedTotalDeduction += (1000 * (i + 1)) * quantity;

            Future<OrderInfo> future = executorService.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();

                    OrderCommand.OrderItemCommand orderItemCommand = new OrderCommand.OrderItemCommand(
                            productId,
                            quantity
                    );

                    OrderCommand.CreateOrderCommand createOrderCommand = new OrderCommand.CreateOrderCommand(
                            loginId,
                            List.of(orderItemCommand),
                            null,
                            DEFAULT_DELIVERY_COMMAND
                    );

                    return orderLockFacade.createOrder(createOrderCommand);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            futures.add(future);
        }

        List<OrderInfo> results = new ArrayList<>();
        for (Future<OrderInfo> future : futures) {
            results.add(future.get());
        }

        executorService.shutdown();

        // assert
        assertThat(results).hasSize(orderCount);

        PointInfo finalPointInfo = pointFacade.getPointInfo(loginId);
        int expectedFinalPoint = initialPoint - expectedTotalDeduction;
        assertThat(finalPointInfo.totalPoint()).isEqualTo(expectedFinalPoint);

        for (int i = 0; i < orderCount; i++) {
            OrderInfo orderInfo = results.get(i);
            int expectedOrderPrice = (1000 * (i + 1)) * quantities[i];
            int actualOrderPrice = orderInfo.orderItems().stream()
                    .mapToInt(item -> item.price() * item.quantity())
                    .sum();
            assertThat(actualOrderPrice).isEqualTo(expectedOrderPrice);
        }
    }

    @DisplayName("동일한 유저가 1,000원의 상품을 각각 1, 2, 3, 4, 5개씩 동시에 주문하면, 총 15,000원이 차감되어야 한다.")
    @Test
    void shouldDeductPointCorrectly_whenSameUserCreatesOrdersWithSameProductConcurrently() throws Exception {
        // arrange
        int orderCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(orderCount);
        CountDownLatch latch = new CountDownLatch(orderCount);
        List<Future<OrderInfo>> futures = new ArrayList<>();

        Long sameProductId = productIds.get(0);
        int productPrice = 1000;
        int[] quantities = {1, 2, 3, 4, 5};
        int expectedTotalDeduction = 0;
        for (int quantity : quantities) {
            expectedTotalDeduction += productPrice * quantity;
        }

        // act
        for (int i = 0; i < orderCount; i++) {
            final int quantity = quantities[i];

            Future<OrderInfo> future = executorService.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();

                    OrderCommand.OrderItemCommand orderItemCommand = new OrderCommand.OrderItemCommand(
                            sameProductId,
                            quantity
                    );

                    OrderCommand.CreateOrderCommand createOrderCommand = new OrderCommand.CreateOrderCommand(
                            loginId,
                            List.of(orderItemCommand),
                            null,
                            DEFAULT_DELIVERY_COMMAND
                    );

                    return orderLockFacade.createOrder(createOrderCommand);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            futures.add(future);
        }

        List<OrderInfo> results = new ArrayList<>();
        for (Future<OrderInfo> future : futures) {
            results.add(future.get());
        }

        executorService.shutdown();

        // assert
        assertThat(results).hasSize(orderCount);

        // assert
        PointInfo finalPointInfo = pointFacade.getPointInfo(loginId);
        int expectedFinalPoint = initialPoint - expectedTotalDeduction;
        assertThat(finalPointInfo.totalPoint()).isEqualTo(expectedFinalPoint);

        // assert
        for (int i = 0; i < orderCount; i++) {
            OrderInfo orderInfo = results.get(i);
            int expectedOrderPrice = productPrice * quantities[i];
            int actualOrderPrice = orderInfo.orderItems().stream()
                    .mapToInt(item -> item.price() * item.quantity())
                    .sum();
            assertThat(actualOrderPrice).isEqualTo(expectedOrderPrice);
        }
    }

    @DisplayName("재고가 3개인 상품을 5명이 동시에 주문하면, 3명만 성공하고 2명은 실패해야 하며 재고는 0이 되어야 한다.")
    @Test
    void shouldSucceedOnlyAvailableStock_whenOrdersExceedStockConcurrently() throws InterruptedException {
        // arrange
        int stockQuantity = 3;
        int concurrentRequestCount = 5;

        Product limitedProduct = Product.createProduct(
                "상품",
                1L,
                Price.createPrice(1000),
                LikeCount.createLikeCount(0),
                Stock.createStock(stockQuantity)
        );
        productRepository.saveProduct(limitedProduct);
        Long limitedProductId = limitedProduct.getId();

        List<String> buyerLoginIds = new ArrayList<>();
        for (int i = 0; i < concurrentRequestCount; i++) {
            String buyerId = "buyer" + i;
            userService.signup(new UserCommand.SignupCommand(buyerId, "buyer" + i + "@test.com", "2000-01-01", "M"));
            pointFacade.chargePoint(new PointCommand.ChargeCommand(buyerId, 10000));
            buyerLoginIds.add(buyerId);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(concurrentRequestCount);
        CountDownLatch latch = new CountDownLatch(concurrentRequestCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        // act
        for (String buyerId : buyerLoginIds) {
            executorService.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();

                    OrderCommand.OrderItemCommand orderItemCommand = new OrderCommand.OrderItemCommand(
                            limitedProductId, 1);

                    OrderCommand.CreateOrderCommand createOrderCommand = new OrderCommand.CreateOrderCommand(
                            buyerId, List.of(orderItemCommand), null, DEFAULT_DELIVERY_COMMAND);

                    orderLockFacade.createOrder(createOrderCommand);

                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            });
        }

        executorService.shutdown();
        if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
            executorService.shutdownNow();
        }

        // assert
        assertThat(successCount.get()).isEqualTo(stockQuantity);

        assertThat(failCount.get()).isEqualTo(concurrentRequestCount - stockQuantity);

        Product finalProduct = productService.findProductById(limitedProductId).orElseThrow();
        assertThat(finalProduct.getStock().getQuantity()).isZero();
    }

    @DisplayName("동일한 쿠폰을 여러 주문에서 동시에 사용하려고 할 경우, 하나만 성공하고 나머지는 실패해야 한다.")
    @Test
    void shouldSucceedOnlyOneOrder_whenSameCouponIsUsedConcurrently() throws Exception {
        // arrange
        int concurrentRequestCount = 5;
        Long userId = userService.findUserByLoginId(loginId).orElseThrow().getId();

        Coupon coupon = Coupon.createCoupon(userId, "쿠폰", Discount.createFixed(500));
        couponService.saveCoupon(coupon);
        Long couponId = coupon.getId();

        Long productId = productIds.get(0);
        int productPrice = 1000;
        int quantity = 1;
        int discountAmount = 500;
        int expectedTotalDeduction = (productPrice * quantity - discountAmount);

        ExecutorService executorService = Executors.newFixedThreadPool(concurrentRequestCount);
        CountDownLatch latch = new CountDownLatch(concurrentRequestCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        // act
        for (int i = 0; i < concurrentRequestCount; i++) {
            executorService.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();

                    OrderCommand.OrderItemCommand orderItemCommand = new OrderCommand.OrderItemCommand(
                            productId,
                            quantity
                    );

                    OrderCommand.CreateOrderCommand createOrderCommand = new OrderCommand.CreateOrderCommand(
                            loginId,
                            List.of(orderItemCommand),
                            couponId,
                            DEFAULT_DELIVERY_COMMAND
                    );

                    orderLockFacade.createOrder(createOrderCommand);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            });
        }

        executorService.shutdown();
        if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
            executorService.shutdownNow();
        }

        // assert
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(concurrentRequestCount - 1);

        Coupon finalCoupon = couponService.findCouponByIdAndUserId(couponId, userId).orElseThrow();
        assertThat(finalCoupon.isUsed()).isTrue();

        PointInfo finalPointInfo = pointFacade.getPointInfo(loginId);
        int expectedFinalPoint = initialPoint - expectedTotalDeduction;
        assertThat(finalPointInfo.totalPoint()).isEqualTo(expectedFinalPoint);
    }

    @DisplayName("서로 다른 쿠폰을 여러 주문에서 동시에 사용할 경우, 모두 성공해야 한다.")
    @Test
    void shouldSucceedAllOrders_whenDifferentCouponsAreUsedConcurrently() throws Exception {
        // arrange
        int orderCount = 5;
        Long userId = userService.findUserByLoginId(loginId).orElseThrow().getId();
        
        List<Long> couponIds = new ArrayList<>();
        for (int i = 0; i < orderCount; i++) {
            Coupon coupon = Coupon.createCoupon(userId, "쿠폰" + i, Discount.createFixed(500 * (i + 1)));
            couponService.saveCoupon(coupon);
            couponIds.add(coupon.getId());
        }

        ExecutorService executorService = Executors.newFixedThreadPool(orderCount);
        CountDownLatch latch = new CountDownLatch(orderCount);
        List<Future<OrderInfo>> futures = new ArrayList<>();

        int expectedTotalDeduction = 0;
        int[] quantities = {1, 1, 1, 1, 1};

        // act
        for (int i = 0; i < orderCount; i++) {
            final Long productId = productIds.get(i);
            final Long couponId = couponIds.get(i);
            final int quantity = quantities[i];
            final int productPrice = 1000 * (i + 1);
            final int discountAmount = 500 * (i + 1);
            
            expectedTotalDeduction += (productPrice * quantity - discountAmount);

            Future<OrderInfo> future = executorService.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();

                    OrderCommand.OrderItemCommand orderItemCommand = new OrderCommand.OrderItemCommand(
                            productId,
                            quantity
                    );

                    OrderCommand.CreateOrderCommand createOrderCommand = new OrderCommand.CreateOrderCommand(
                            loginId,
                            List.of(orderItemCommand),
                            couponId,
                            DEFAULT_DELIVERY_COMMAND
                    );

                    return orderLockFacade.createOrder(createOrderCommand);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            futures.add(future);
        }

        List<OrderInfo> results = new ArrayList<>();
        for (Future<OrderInfo> future : futures) {
            results.add(future.get());
        }

        executorService.shutdown();

        // assert
        assertThat(results).hasSize(orderCount);

        for (Long couponId : couponIds) {
            Coupon usedCoupon = couponService.findCouponByIdAndUserId(couponId, userId).orElseThrow();
            assertThat(usedCoupon.isUsed()).isTrue();
        }

        PointInfo finalPointInfo = pointFacade.getPointInfo(loginId);
        int expectedFinalPoint = initialPoint - expectedTotalDeduction;
        assertThat(finalPointInfo.totalPoint()).isEqualTo(expectedFinalPoint);
    }
}

