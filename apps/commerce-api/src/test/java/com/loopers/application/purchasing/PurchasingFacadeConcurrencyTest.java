package com.loopers.application.purchasing;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponRepository;
import com.loopers.domain.coupon.CouponType;
import com.loopers.domain.coupon.UserCoupon;
import com.loopers.domain.coupon.UserCouponRepository;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.Point;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.loopers.testcontainers.MySqlTestContainersConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PurchasingFacade 동시성 테스트
 * <p>
 * 여러 스레드에서 동시에 주문 요청을 보내도 데이터 일관성이 유지되는지 검증합니다.
 * - 포인트 차감의 정확성
 * - 재고 차감의 정확성
 * - 쿠폰 사용의 중복 방지 (예시)
 * </p>
 */
@SpringBootTest
@Import(MySqlTestContainersConfig.class)
@DisplayName("PurchasingFacade 동시성 테스트")
class PurchasingFacadeConcurrencyTest {

    @Autowired
    private PurchasingFacade purchasingFacade;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    private User createAndSaveUser(String userId, String email, long point) {
        User user = User.of(userId, email, "1990-01-01", Gender.MALE, Point.of(point));
        return userRepository.save(user);
    }

    private Brand createAndSaveBrand(String brandName) {
        Brand brand = Brand.of(brandName);
        return brandRepository.save(brand);
    }

    private Product createAndSaveProduct(String productName, int price, int stock, Long brandId) {
        Product product = Product.of(productName, price, stock, brandId);
        return productRepository.save(product);
    }

    private Coupon createAndSaveCoupon(String code, CouponType type, Integer discountValue) {
        Coupon coupon = Coupon.of(code, type, discountValue);
        return couponRepository.save(coupon);
    }

    private UserCoupon createAndSaveUserCoupon(Long userId, Coupon coupon) {
        UserCoupon userCoupon = UserCoupon.of(userId, coupon);
        return userCouponRepository.save(userCoupon);
    }

    @Test
    @DisplayName("동일한 유저가 서로 다른 주문을 동시에 수행해도, 포인트가 정상적으로 차감되어야 한다")
    void concurrencyTest_pointShouldProperlyDecreaseWhenOrderCreated() throws InterruptedException {
        // arrange
        User user = createAndSaveUser("testuser", "test@example.com", 100_000L);
        String userId = user.getUserId();
        Brand brand = createAndSaveBrand("테스트 브랜드");

        int orderCount = 5;
        List<Product> products = new ArrayList<>();
        for (int i = 0; i < orderCount; i++) {
            products.add(createAndSaveProduct("상품" + i, 10_000, 100, brand.getId()));
        }

        ExecutorService executorService = Executors.newFixedThreadPool(orderCount);
        CountDownLatch latch = new CountDownLatch(orderCount);
        AtomicInteger successCount = new AtomicInteger(0);
        List<Exception> exceptions = new ArrayList<>();

        // act
        for (int i = 0; i < orderCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    List<OrderItemCommand> commands = List.of(
                        OrderItemCommand.of(products.get(index).getId(), 1)
                    );
                    purchasingFacade.createOrder(userId, commands);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    synchronized (exceptions) {
                        exceptions.add(e);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executorService.shutdown();

        // assert
        User savedUser = userRepository.findByUserId(userId);
        long expectedRemainingPoint = 100_000L - (10_000L * orderCount);

        assertThat(successCount.get()).isEqualTo(orderCount);
        assertThat(exceptions).isEmpty();
        assertThat(savedUser.getPoint().getValue()).isEqualTo(expectedRemainingPoint);
    }

    @Test
    @DisplayName("동일한 상품에 대해 여러 주문이 동시에 요청되어도, 재고가 정상적으로 차감되어야 한다")
    void concurrencyTest_stockShouldBeProperlyDecreasedWhenOrdersCreated() throws InterruptedException {
        // arrange
        User user = createAndSaveUser("testuser", "test@example.com", 1_000_000L);
        String userId = user.getUserId();
        Brand brand = createAndSaveBrand("테스트 브랜드");
        Product product = createAndSaveProduct("테스트 상품", 10_000, 100, brand.getId());
        Long productId = product.getId();

        int orderCount = 10;
        int quantityPerOrder = 5;

        ExecutorService executorService = Executors.newFixedThreadPool(orderCount);
        CountDownLatch latch = new CountDownLatch(orderCount);
        AtomicInteger successCount = new AtomicInteger(0);
        List<Exception> exceptions = new ArrayList<>();

        // act
        for (int i = 0; i < orderCount; i++) {
            executorService.submit(() -> {
                try {
                    List<OrderItemCommand> commands = List.of(
                        OrderItemCommand.of(productId, quantityPerOrder)
                    );
                    purchasingFacade.createOrder(userId, commands);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    synchronized (exceptions) {
                        exceptions.add(e);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executorService.shutdown();

        // assert
        Product savedProduct = productRepository.findById(productId).orElseThrow();
        int expectedStock = 100 - (successCount.get() * quantityPerOrder);

        assertThat(savedProduct.getStock()).isEqualTo(expectedStock);
        assertThat(successCount.get() + exceptions.size()).isEqualTo(orderCount);
    }

    @Test
    @DisplayName("동일한 쿠폰으로 여러 기기에서 동시에 주문해도, 쿠폰은 단 한번만 사용되어야 한다")
    void concurrencyTest_couponShouldBeUsedOnlyOnceWhenOrdersCreated() throws InterruptedException {
        // arrange
        User user = createAndSaveUser("testuser", "test@example.com", 100_000L);
        String userId = user.getUserId();
        Brand brand = createAndSaveBrand("테스트 브랜드");
        Product product = createAndSaveProduct("테스트 상품", 10_000, 100, brand.getId());

        // 정액 쿠폰 생성 (5,000원 할인)
        Coupon coupon = createAndSaveCoupon("COUPON001", CouponType.FIXED_AMOUNT, 5_000);
        String couponCode = coupon.getCode();

        // 사용자에게 쿠폰 지급
        UserCoupon userCoupon = createAndSaveUserCoupon(user.getId(), coupon);

        int concurrentRequestCount = 10; // 요구사항: 10개 스레드

        ExecutorService executorService = Executors.newFixedThreadPool(concurrentRequestCount);
        CountDownLatch latch = new CountDownLatch(concurrentRequestCount);
        AtomicInteger successCount = new AtomicInteger(0);
        List<Exception> exceptions = new ArrayList<>();

        // act
        for (int i = 0; i < concurrentRequestCount; i++) {
            executorService.submit(() -> {
                try {
                    List<OrderItemCommand> commands = List.of(
                        new OrderItemCommand(product.getId(), 1, couponCode)
                    );
                    purchasingFacade.createOrder(userId, commands);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    synchronized (exceptions) {
                        exceptions.add(e);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executorService.shutdown();

        // assert
        // 쿠폰은 정확히 1번만 사용되어야 함
        UserCoupon savedUserCoupon = userCouponRepository.findByUserIdAndCouponCode(user.getId(), couponCode)
            .orElseThrow();
        assertThat(savedUserCoupon.isAvailable()).isFalse(); // 사용됨
        assertThat(savedUserCoupon.getIsUsed()).isTrue();

        // 성공한 주문은 1개만 있어야 함 (나머지는 쿠폰 중복 사용으로 실패)
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(exceptions).hasSize(concurrentRequestCount - 1);

        // 성공한 주문의 할인 금액이 적용되었는지 확인
        List<Order> orders = orderRepository.findAllByUserId(user.getId());
        assertThat(orders).hasSize(1);
        Order order = orders.get(0);
        assertThat(order.getCouponCode()).isEqualTo(couponCode);
        assertThat(order.getDiscountAmount()).isEqualTo(5_000);
        assertThat(order.getTotalAmount()).isEqualTo(5_000); // 10,000 - 5,000 = 5,000
    }

    @Test
    @DisplayName("주문 취소 중 다른 스레드가 재고를 변경해도, 재고 원복이 정확하게 이루어져야 한다")
    void concurrencyTest_cancelOrderShouldRestoreStockAccuratelyDuringConcurrentStockChanges() throws InterruptedException {
        // arrange
        User user = createAndSaveUser("testuser", "test@example.com", 1_000_000L);
        String userId = user.getUserId();
        Brand brand = createAndSaveBrand("테스트 브랜드");
        Product product = createAndSaveProduct("테스트 상품", 10_000, 100, brand.getId());
        Long productId = product.getId();
        
        // 주문 생성 (재고 5개 차감)
        int orderQuantity = 5;
        List<OrderItemCommand> commands = List.of(
            OrderItemCommand.of(productId, orderQuantity)
        );
        OrderInfo orderInfo = purchasingFacade.createOrder(userId, commands);
        Long orderId = orderInfo.orderId();
        
        // 주문 취소 전 재고 확인 (100 - 5 = 95)
        Product productBeforeCancel = productRepository.findById(productId).orElseThrow();
        int stockBeforeCancel = productBeforeCancel.getStock();
        assertThat(stockBeforeCancel).isEqualTo(95);
        
        // 주문 조회
        Order order = orderRepository.findById(orderId).orElseThrow();
        
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        CountDownLatch latch = new CountDownLatch(3);
        AtomicInteger cancelSuccess = new AtomicInteger(0);
        AtomicInteger orderSuccess = new AtomicInteger(0);
        List<Exception> exceptions = new ArrayList<>();
        
        // act
        // 스레드 1: 주문 취소 (재고 원복)
        executorService.submit(() -> {
            try {
                purchasingFacade.cancelOrder(order, user);
                cancelSuccess.incrementAndGet();
            } catch (Exception e) {
                synchronized (exceptions) {
                    exceptions.add(e);
                }
            } finally {
                latch.countDown();
            }
        });
        
        // 스레드 2, 3: 취소 중간에 다른 주문 생성 (재고 추가 차감)
        for (int i = 0; i < 2; i++) {
            executorService.submit(() -> {
                try {
                    Thread.sleep(10); // 취소가 시작된 후 실행되도록 약간의 지연
                    List<OrderItemCommand> otherCommands = List.of(
                        OrderItemCommand.of(productId, 3)
                    );
                    purchasingFacade.createOrder(userId, otherCommands);
                    orderSuccess.incrementAndGet();
                } catch (Exception e) {
                    synchronized (exceptions) {
                        exceptions.add(e);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executorService.shutdown();
        
        // assert
        // findByIdForUpdate로 인해 비관적 락이 적용되어 재고 원복이 정확하게 이루어져야 함
        Product finalProduct = productRepository.findById(productId).orElseThrow();
        int finalStock = finalProduct.getStock();
        
        // 시나리오:
        // 1. 초기 재고: 100
        // 2. 첫 주문: 95 (100 - 5)
        // 3. 주문 취소: 100 (95 + 5) - 비관적 락으로 정확한 재고 조회 후 원복
        // 4. 다른 주문 2개: 각각 3개씩 차감
        //    - 취소와 동시에 실행되면 락 대기 후 순차 처리
        //    - 최종 재고: 100 - 3 - 3 = 94 (취소로 5개 원복 후 2개 주문으로 6개 차감)
        
        assertThat(cancelSuccess.get()).isEqualTo(1);
        // 취소가 성공했고, 비관적 락으로 인해 정확한 재고가 원복되었는지 확인
        // 취소로 5개가 원복되고, 다른 주문 2개로 6개가 차감되므로: 95 + 5 - 6 = 94
        int expectedStock = stockBeforeCancel + orderQuantity - (orderSuccess.get() * 3);
        assertThat(finalStock).isEqualTo(expectedStock);
        
        // 예외가 발생하지 않았는지 확인
        assertThat(exceptions).isEmpty();
    }
}

