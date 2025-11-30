package com.loopers.domain.order;

import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderPlaceCommand;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponRepository;
import com.loopers.domain.coupon.DiscountType;
import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OrderConcurrencyTest {

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private UserService userService;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PointRepository pointRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private User user;

    @BeforeEach
    void setUp() {
        databaseCleanUp.truncateAllTables();
        user = userService.signUp("testUser", "test@mail.com", "1990-01-01", Gender.MALE);
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    private Product setupProduct(int stock, long price) {
        Brand brand = brandRepository.save(Brand.create("Brand"));
        return productRepository.save(Product.create("Product", price, stock, brand));
    }

    private void setupPoint(long amount) {
        pointRepository.save(Point.create(user.getUserIdValue(), amount));
    }

    private Coupon setupCoupon(long amount) {
        return couponRepository.save(Coupon.create(user, amount + "원 할인", DiscountType.FIXED_AMOUNT, amount));
    }

    @DisplayName("100명이 재고 100개 상품을 동시 주문하면, 모두 성공하고 재고는 정확히 0이 된다")
    @Test
    void testStockConcurrency() throws InterruptedException {
        // given
        Product product = setupProduct(100, 1000L);
        setupPoint(100 * 1000L + 1000L);

        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // act
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    OrderPlaceCommand command = new OrderPlaceCommand(
                            user.getUserIdValue(),
                            List.of(new OrderPlaceCommand.OrderItemCommand(product.getId(), 1)),
                            null
                    );
                    orderFacade.placeOrder(command);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("재고 동시성 테스트 실패: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executor.shutdown();

        // assert
        Product foundProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(successCount.get()).isEqualTo(100);
        assertThat(foundProduct.getStockValue()).isZero();
    }

    @DisplayName("동일 유저가 동일 쿠폰으로 100번 동시 주문하면, 1번만 성공하고 쿠폰은 사용 처리된다")
    @Test
    void testCouponConcurrency() throws InterruptedException {
        // given
        Product product = setupProduct(1000, 1000L);
        setupPoint(100 * 1000L);
        Coupon coupon = setupCoupon(100L); // 100원 할인 쿠폰

        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // act
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    OrderPlaceCommand command = new OrderPlaceCommand(
                            user.getUserIdValue(),
                            List.of(new OrderPlaceCommand.OrderItemCommand(product.getId(), 1)),
                            coupon.getId() // 동일 쿠폰
                    );
                    orderFacade.placeOrder(command);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet(); // 락 경합 또는 이미 사용된 쿠폰
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executor.shutdown();

        // assert
        Coupon foundCoupon = couponRepository.findById(coupon.getId()).orElseThrow();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(99);
        assertThat(foundCoupon.getIsUsed()).isTrue();
    }

    @DisplayName("동일 유저가 100개 주문을 동시 요청하면, 포인트는 정확한 금액만큼 차감된다")
    @Test
    void testPointConcurrency() throws InterruptedException {
        // given
        Product product = setupProduct(1000, 100L);
        long initialPoint = 100L * 100L;
        setupPoint(initialPoint);

        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // act
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    OrderPlaceCommand command = new OrderPlaceCommand(
                            user.getUserIdValue(),
                            List.of(new OrderPlaceCommand.OrderItemCommand(product.getId(), 1)),
                            null
                    );
                    orderFacade.placeOrder(command);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("포인트 동시성 테스트 실패: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executor.shutdown();

        // assert
        Point foundPoint = pointRepository.findByUserId(user.getUserIdValue()).orElseThrow();
        assertThat(successCount.get()).isEqualTo(100);
        assertThat(foundPoint.getBalanceValue()).isZero();
    }
}
