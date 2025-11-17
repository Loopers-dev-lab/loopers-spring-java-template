package com.loopers.application.concurrency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.loopers.application.coupon.CouponService;
import com.loopers.application.like.LikeService;
import com.loopers.application.order.OrderCommand;
import com.loopers.application.order.OrderCommand.OrderItemRequest;
import com.loopers.application.order.OrderFacade;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponType;
import com.loopers.domain.coupon.UserCoupon;
import com.loopers.domain.like.Like;
import com.loopers.domain.point.Point;
import com.loopers.domain.product.Product;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.coupon.CouponJpaRepository;
import com.loopers.infrastructure.coupon.UserCouponJpaRepository;
import com.loopers.infrastructure.like.LikeJpaRepository;
import com.loopers.infrastructure.point.PointJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.support.error.CoreException;
import com.loopers.utils.DatabaseCleanUp;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@DisplayName("동시성 제어 통합 테스트")
class ConcurrencyTest {

    @Autowired
    private LikeService likeService;

    @Autowired
    private CouponService couponService;

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private LikeJpaRepository likeJpaRepository;

    @Autowired
    private UserCouponJpaRepository userCouponJpaRepository;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private PointJpaRepository pointJpaRepository;

    @Autowired
    private CouponJpaRepository couponJpaRepository;

    @Autowired
    private BrandJpaRepository brandJpaRepository;

    @Autowired
    private com.loopers.domain.product.ProductRepository productRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("동시성 테스트 1: 동일 상품에 여러 사용자가 동시에 좋아요/싫어요 요청")
    @Test
    void concurrentLikeAndUnlike() throws InterruptedException {
        // given
        Brand brand = Brand.builder()
            .name("테스트 브랜드")
            .description("테스트용 브랜드")
            .build();
        brand = ((org.springframework.data.jpa.repository.JpaRepository<Brand, Long>) brandJpaRepository).save(brand);

        Product product = Product.builder()
            .brand(brand)
            .name("테스트 상품")
            .price(BigDecimal.valueOf(10000))
            .stock(100)
            .description("테스트용 상품")
            .build();
        product = ((org.springframework.data.jpa.repository.JpaRepository<Product, Long>) productJpaRepository).save(product);

        final Long productId = product.getId(); // 최종 변수로 ID 저장

        int threadCount = 10; // 10명의 사용자
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when - 10명의 사용자가 동시에 좋아요 요청
        for (int i = 0; i < threadCount; i++) {
            final String userId = "user" + i;
            executorService.submit(() -> {
                try {
                    likeService.like(userId, productId);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then - 10개의 좋아요가 모두 정상적으로 저장되었는지 확인
        List<Like> likes = likeJpaRepository.findAll();
        Long likeCount = likeService.getLikeCount(product.getId());

        assertAll(
            () -> assertThat(likes).hasSize(10),
            () -> assertThat(likeCount).isEqualTo(10L)
        );

        // when - 동일한 사용자들이 동시에 싫어요 요청
        ExecutorService executorService2 = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch2 = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final String userId = "user" + i;
            executorService2.submit(() -> {
                try {
                    likeService.unlike(userId, productId);
                } finally {
                    latch2.countDown();
                }
            });
        }

        latch2.await();
        executorService2.shutdown();

        // then - 모든 좋아요가 취소되었는지 확인 (soft delete 고려)
        Long finalLikeCount = likeService.getLikeCount(productId);

        // 각 사용자의 좋아요 상태 확인
        AtomicInteger activeLikeCount = new AtomicInteger(0);
        for (int i = 0; i < threadCount; i++) {
            if (likeService.isLiked("user" + i, productId)) {
                activeLikeCount.incrementAndGet();
            }
        }

        assertAll(
            () -> assertThat(activeLikeCount.get()).isEqualTo(0),
            () -> assertThat(finalLikeCount).isEqualTo(0L)
        );
    }

    @DisplayName("동시성 테스트 2: 동일한 쿠폰을 여러 기기에서 동시에 사용 시도")
    @Test
    void concurrentCouponUsage() throws InterruptedException {
        // given
        Brand brand = Brand.builder()
            .name("테스트 브랜드")
            .description("테스트용 브랜드")
            .build();
        brand = ((org.springframework.data.jpa.repository.JpaRepository<Brand, Long>) brandJpaRepository).save(brand);

        Product product = Product.builder()
            .brand(brand)
            .name("테스트 상품")
            .price(BigDecimal.valueOf(10000))
            .stock(100)
            .description("테스트용 상품")
            .build();
        product = ((org.springframework.data.jpa.repository.JpaRepository<Product, Long>) productJpaRepository).save(product);

        Coupon coupon = Coupon.builder()
            .name("5000원 할인 쿠폰")
            .type(CouponType.FIXED_AMOUNT)
            .discountValue(BigDecimal.valueOf(5000))
            .description("테스트용 쿠폰")
            .build();
        coupon = ((org.springframework.data.jpa.repository.JpaRepository<Coupon, Long>) couponJpaRepository).save(coupon);

        String userId = "user123";
        UserCoupon userCoupon = UserCoupon.builder()
            .userId(userId)
            .coupon(coupon)
            .build();
        userCoupon = ((org.springframework.data.jpa.repository.JpaRepository<UserCoupon, Long>) userCouponJpaRepository).save(userCoupon);

        Point point = Point.builder()
            .userId(userId)
            .balance(BigDecimal.valueOf(100000)) // 충분한 포인트
            .build();
        point = ((org.springframework.data.jpa.repository.JpaRepository<Point, Long>) pointJpaRepository).save(point);

        final Long productId = product.getId(); // 최종 변수로 ID 저장
        final Long userCouponId = userCoupon.getId(); // 최종 변수로 ID 저장

        int threadCount = 5; // 5개의 기기에서 동시 요청
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 동일한 쿠폰으로 5개의 주문을 동시에 시도
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    OrderCommand.Create command = new OrderCommand.Create(
                        List.of(new OrderItemRequest(productId, 1)),
                        userCouponId
                    );
                    orderFacade.createOrder(userId, command);
                    successCount.incrementAndGet();
                } catch (CoreException e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then - 1개만 성공하고 나머지는 실패해야 함 (낙관적 락 또는 비관적 락에 의해)
        UserCoupon finalUserCoupon = userCouponJpaRepository.findById(userCoupon.getId())
            .orElseThrow();

        assertAll(
            () -> assertThat(successCount.get()).isEqualTo(1),
            () -> assertThat(failCount.get()).isEqualTo(4),
            () -> assertThat(finalUserCoupon.isUsed()).isTrue(),
            () -> assertThat(finalUserCoupon.getUsedAt()).isNotNull()
        );
    }

    @DisplayName("동시성 테스트 3: 동일 유저의 서로 다른 주문 동시 수행 (포인트 차감)")
    @Test
    void concurrentOrdersWithSameUserPoint() throws InterruptedException {
        // given
        Brand brand = Brand.builder()
            .name("테스트 브랜드")
            .description("테스트용 브랜드")
            .build();
        brand = ((org.springframework.data.jpa.repository.JpaRepository<Brand, Long>) brandJpaRepository).save(brand);

        Product product = Product.builder()
            .brand(brand)
            .name("테스트 상품")
            .price(BigDecimal.valueOf(10000))
            .stock(100)
            .description("테스트용 상품")
            .build();
        product = ((org.springframework.data.jpa.repository.JpaRepository<Product, Long>) productJpaRepository).save(product);

        String userId = "user123";
        BigDecimal initialBalance = BigDecimal.valueOf(50000);
        Point point = Point.builder()
            .userId(userId)
            .balance(initialBalance)
            .build();
        point = ((org.springframework.data.jpa.repository.JpaRepository<Point, Long>) pointJpaRepository).save(point);

        final Long productId = product.getId(); // 최종 변수로 ID 저장

        int threadCount = 10; // 10개의 주문 동시 요청
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 동일한 사용자가 10,000원짜리 상품을 10번 동시에 주문 시도
        // 초기 잔액 50,000원으로는 5개까지만 성공해야 함
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    OrderCommand.Create command = new OrderCommand.Create(
                        List.of(new OrderItemRequest(productId, 1)),
                        null // 쿠폰 없이
                    );
                    orderFacade.createOrder(userId, command);
                    successCount.incrementAndGet();
                } catch (CoreException e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then - 5개만 성공하고 나머지는 실패해야 함 (포인트 부족)
        Point finalPoint = pointJpaRepository.findByUserId(userId).orElseThrow();

        assertAll(
            () -> assertThat(successCount.get()).isEqualTo(5),
            () -> assertThat(failCount.get()).isEqualTo(5),
            () -> assertThat(finalPoint.getBalance()).isEqualByComparingTo(BigDecimal.ZERO)
        );
    }

    @DisplayName("동시성 테스트 4: 동일 상품 여러 주문 동시 요청 (재고 차감)")
    @Test
    void concurrentOrdersWithSameProductStock() throws InterruptedException {
        // given
        Brand brand = Brand.builder()
            .name("테스트 브랜드")
            .description("테스트용 브랜드")
            .build();
        brand = ((org.springframework.data.jpa.repository.JpaRepository<Brand, Long>) brandJpaRepository).save(brand);

        int initialStock = 10;
        Product product = Product.builder()
            .brand(brand)
            .name("인기 상품")
            .price(BigDecimal.valueOf(10000))
            .stock(initialStock)
            .description("재고가 10개밖에 없는 인기 상품")
            .build();
        product = ((org.springframework.data.jpa.repository.JpaRepository<Product, Long>) productJpaRepository).save(product);

        final Long productId = product.getId(); // 최종 변수로 ID 저장

        int threadCount = 20; // 20명의 사용자가 동시에 구매 시도
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // 각 사용자마다 충분한 포인트 지급
        for (int i = 0; i < threadCount; i++) {
            String userId = "user" + i;
            Point userPoint = Point.builder()
                .userId(userId)
                .balance(BigDecimal.valueOf(100000))
                .build();
            userPoint = ((org.springframework.data.jpa.repository.JpaRepository<Point, Long>) pointJpaRepository).save(userPoint);
        }

        // when - 20명의 사용자가 동시에 1개씩 구매 시도
        // 재고가 10개이므로 10개만 성공해야 함
        for (int i = 0; i < threadCount; i++) {
            final String userId = "user" + i;
            executorService.submit(() -> {
                try {
                    OrderCommand.Create command = new OrderCommand.Create(
                        List.of(new OrderItemRequest(productId, 1)),
                        null // 쿠폰 없이
                    );
                    orderFacade.createOrder(userId, command);
                    successCount.incrementAndGet();
                } catch (CoreException e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then - 10개만 성공하고 나머지는 실패해야 함 (재고 부족)
        Product finalProduct = productRepository.findById(product.getId()).orElseThrow();

        assertAll(
            () -> assertThat(successCount.get()).isEqualTo(10),
            () -> assertThat(failCount.get()).isEqualTo(10),
            () -> assertThat(finalProduct.getStock()).isEqualTo(0)
        );
    }
}
