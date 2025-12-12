package com.loopers.domain.order;

import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderPlaceCommand;
import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponRepository;
import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.User;
import com.loopers.fixture.TestFixture;
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
    private ProductRepository productRepository;

    @Autowired
    private PointRepository pointRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private TestFixture testFixture;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private User user;

    @BeforeEach
    void setUp() {
        databaseCleanUp.truncateAllTables();
        user = testFixture.createUser("orderConcurrencyUser");
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("100명이 재고 100개 상품을 동시 주문하면, 모두 성공하고 재고는 정확히 0이 된다")
    @Test
    void testStockConcurrency() throws InterruptedException {
        // 픽스처를 이용한 테스트 데이터 생성
        var brand = testFixture.createBrand();
        Product product = testFixture.createProduct(brand, 1000L, 100);
        testFixture.createPoint(user.getId(), 100 * 1000L + 1000L);

        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    OrderPlaceCommand command = new OrderPlaceCommand(
                            user.getLoginIdValue(),
                            List.of(new OrderPlaceCommand.OrderItemCommand(product.getId(), 1)),
                            null,
                            OrderPlaceCommand.PaymentMethod.POINT,
                            null
                    );
                    orderFacade.createOrder(command);
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

        Product foundProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(successCount.get()).isEqualTo(100);
        assertThat(foundProduct.getStockValue()).isZero();
    }

    @DisplayName("동일 유저가 동일 쿠폰으로 100번 동시 주문하면, 1번만 성공하고 쿠폰은 사용 처리된다")
    @Test
    void testCouponConcurrency() throws InterruptedException {
        var brand = testFixture.createBrand();
        Product product = testFixture.createProduct(brand, 1000L, 1000);
        testFixture.createPoint(user.getId(), 100 * 1000L);
        Coupon coupon = testFixture.createFixedAmountCoupon(user, 100L);

        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    OrderPlaceCommand command = new OrderPlaceCommand(
                            user.getLoginIdValue(),
                            List.of(new OrderPlaceCommand.OrderItemCommand(product.getId(), 1)),
                            coupon.getId(),
                            OrderPlaceCommand.PaymentMethod.POINT,
                            null
                    );
                    orderFacade.createOrder(command);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executor.shutdown();

        Coupon foundCoupon = couponRepository.findById(coupon.getId()).orElseThrow();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(99);
        assertThat(foundCoupon.getIsUsed()).isTrue();
    }

    @DisplayName("동일 유저가 100개 주문을 동시 요청하면, 포인트는 정확한 금액만큼 차감된다")
    @Test
    void testPointConcurrency() throws InterruptedException {
        var brand = testFixture.createBrand();
        Product product = testFixture.createProduct(brand, 100L, 1000);
        long initialPoint = 100L * 100L;
        testFixture.createPoint(user.getId(), initialPoint);

        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    OrderPlaceCommand command = new OrderPlaceCommand(
                            user.getLoginIdValue(),
                            List.of(new OrderPlaceCommand.OrderItemCommand(product.getId(), 1)),
                            null,
                            OrderPlaceCommand.PaymentMethod.POINT,
                            null
                    );
                    orderFacade.createOrder(command);
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

        Point foundPoint = pointRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(successCount.get()).isEqualTo(100);
        assertThat(foundPoint.getBalanceValue()).isZero();
    }
}
