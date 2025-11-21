package com.loopers.domain.coupon;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandStatus;
import com.loopers.domain.product.ProductStatus;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Coupon Service 테스트")
@SpringBootTest
class CouponServiceTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandJpaRepository brandJpaRepository;

    @Autowired
    private UserFacade userFacade;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private User test34;
    private Order testOrder;
    private Long testCouponId;

    @BeforeEach
    void setUp() {
        // 테스트용 User 생성
        UserInfo userInfo = UserInfo.builder()
                .loginId("test34")
                .email("test@test.com")
                .birthday("1990-01-01")
                .gender(Gender.MALE)
                .build();
        userFacade.saveUser(userInfo);

        test34 = userRepository.findByLoginId("test34")
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

        // 테스트용 Order 생성
        testOrder = Order.builder()
                .discountAmount(BigDecimal.ZERO)
                .shippingFee(BigDecimal.ZERO)
                .user(test34)
                .build();
        
        // OrderItem 추가
        testOrder.addOrderItem(savedProduct, 10);

        testOrder = orderService.saveOrder(testOrder)
                .orElseThrow(() -> new RuntimeException("Order 저장 실패"));

        // 테스트용 Coupon 생성
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIXED_AMOUNT)
                .discountValue(BigDecimal.valueOf(5000))
                .user(test34)
                .build();

        Coupon savedCoupon = couponRepository.save(coupon)
                .orElseThrow(() -> new RuntimeException("Coupon 저장 실패"));
        testCouponId = savedCoupon.getId();
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("useCoupon 테스트")
    @Nested
    class UseCouponTest {

        @DisplayName("성공 케이스: 모든 조건을 만족하는 쿠폰 사용 성공")
        @Test
        void useCoupon_withValidCoupon_Success() {
            // arrange
            Order order = createTestOrder();
            BigDecimal initialTotalPrice = order.getTotalPrice();
            BigDecimal expectedDiscount = BigDecimal.valueOf(5000); // FIXED_AMOUNT 5000원

            // act
            couponService.useCoupon(order, testCouponId);

            // assert
            Coupon usedCoupon = couponRepository.findById(testCouponId)
                    .orElseThrow(() -> new RuntimeException("쿠폰을 찾을 수 없습니다"));
            
            assertAll(
                    () -> assertTrue(usedCoupon.getIsUsed(), "쿠폰은 사용된 상태여야 함"),
                    () -> assertNotNull(usedCoupon.getOrder(), "쿠폰의 order는 null이 아니어야 함"),
                    () -> assertEquals(order.getId(), usedCoupon.getOrder().getId(), "쿠폰의 order ID가 일치해야 함"),
                    () -> assertEquals(0, initialTotalPrice.subtract(expectedDiscount).compareTo(order.getFinalAmount()), "주문의 할인이 적용되어야 함")
            );
        }

        @DisplayName("실패 케이스: 존재하지 않는 쿠폰 사용 시 NOT_FOUND 예외 발생")
        @Test
        void useCoupon_withNonExistentCoupon_NotFound() {
            // arrange
            Order order = createTestOrder();
            Long nonExistentCouponId = 99999L;

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () ->
                    couponService.useCoupon(order, nonExistentCouponId)
            );

            assertEquals(ErrorType.NOT_FOUND, exception.getErrorType());
            assertTrue(exception.getCustomMessage().contains("[couponId = " + nonExistentCouponId + "] Coupon을 찾을 수 없습니다"));
        }

        @DisplayName("실패 케이스: 다른 사용자의 쿠폰 사용 시 BAD_REQUEST 예외 발생")
        @Test
        void useCoupon_withOtherUserCoupon_BadRequest() {
            // arrange
            // 다른 사용자 생성
            UserInfo otherUserInfo = UserInfo.builder()
                    .loginId("other34")
                    .email("other@test.com")
                    .birthday("1990-01-01")
                    .gender(Gender.MALE)
                    .build();
            userFacade.saveUser(otherUserInfo);
            User otherUser = userRepository.findByLoginId("other34")
                    .orElseThrow(() -> new RuntimeException("User를 찾을 수 없습니다"));

            // 다른 사용자의 쿠폰 생성
            Coupon otherUserCoupon = Coupon.builder()
                    .couponType(CouponType.FIXED_AMOUNT)
                    .discountValue(BigDecimal.valueOf(5000))
                    .user(otherUser)
                    .build();
            Coupon savedOtherUserCoupon = couponRepository.save(otherUserCoupon)
                    .orElseThrow(() -> new RuntimeException("Coupon 저장 실패"));

            Order order = createTestOrder(); // test34의 주문

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () ->
                    couponService.useCoupon(order, savedOtherUserCoupon.getId())
            );

            assertEquals(ErrorType.BAD_REQUEST, exception.getErrorType());
            assertTrue(exception.getCustomMessage().contains("본인의 쿠폰만 사용할 수 있습니다"));
        }

        @DisplayName("실패 케이스: 이미 사용된 쿠폰 사용 시 BAD_REQUEST 예외 발생")
        @Test
        void useCoupon_withUsedCoupon_BadRequest() {
            // arrange
            Order firstOrder = createTestOrder();
            couponService.useCoupon(firstOrder, testCouponId); // 첫 번째 사용

            Order secondOrder = createTestOrder(); // 두 번째 주문

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () ->
                    couponService.useCoupon(secondOrder, testCouponId)
            );

            assertEquals(ErrorType.BAD_REQUEST, exception.getErrorType());
            assertTrue(exception.getCustomMessage().contains("쿠폰을 사용할 수 없습니다"));
        }

        @DisplayName("실패 케이스: 삭제된 쿠폰 사용 시 BAD_REQUEST 예외 발생")
        @Test
        void useCoupon_withDeletedCoupon_BadRequest() {
            // arrange
            // 쿠폰 삭제 (soft delete)
            Coupon coupon = couponRepository.findById(testCouponId)
                    .orElseThrow(() -> new RuntimeException("쿠폰을 찾을 수 없습니다"));
            coupon.delete();
            couponRepository.save(coupon);

            Order order = createTestOrder();

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () ->
                    couponService.useCoupon(order, testCouponId)
            );

            assertEquals(ErrorType.BAD_REQUEST, exception.getErrorType());
            assertTrue(exception.getCustomMessage().contains("쿠폰을 사용할 수 없습니다"));
        }
    }

    @DisplayName("useCoupon 동시성 테스트")
    @Nested
    class UseCouponConcurrencyTest {

        @DisplayName("성공 케이스: 여러 스레드가 동시에 같은 쿠폰을 사용하려고 할 때 정확히 하나만 성공")
        @Test
        void useCoupon_concurrentRequests_onlyOneSuccess() throws InterruptedException {
            // arrange
            int threadCount = 10;

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);
            List<Exception> exceptions = new ArrayList<>();

            // act
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        // 각 스레드마다 새로운 Order 생성
                        Order order = createTestOrder();
                        couponService.useCoupon(order, testCouponId);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                        exceptions.add(e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // wait for all threads to complete
            assertTrue(latch.await(10, TimeUnit.SECONDS), "모든 스레드가 10초 내에 완료되지 않았습니다.");
            executor.shutdown();

            // assert
            Coupon finalCoupon = couponRepository.findById(testCouponId)
                    .orElseThrow(() -> new RuntimeException("쿠폰을 찾을 수 없습니다"));

            assertEquals(1, successCount.get(),
                    String.format("성공한 스레드 수는 1이어야 함: 실제=%d", successCount.get()));
            assertEquals(threadCount - 1, failureCount.get(),
                    String.format("실패한 스레드 수는 %d이어야 함: 실제=%d", threadCount - 1, failureCount.get()));
            assertTrue(finalCoupon.getIsUsed(),
                    String.format("쿠폰은 사용된 상태여야 함: 실제=%s", finalCoupon.getIsUsed()));

            // 실패한 경우는 쿠폰 사용 불가 예외여야 함
            long couponUnavailableExceptions = exceptions.stream()
                    .filter(e -> e instanceof CoreException)
                    .filter(e -> ((CoreException) e).getErrorType() == ErrorType.BAD_REQUEST)
                    .filter(e -> e.getMessage().contains("쿠폰을 사용할 수 없습니다"))
                    .count();
            assertEquals(failureCount.get(), couponUnavailableExceptions,
                    String.format("쿠폰 사용 불가 예외 수는 실패 수와 일치해야 함: 예상=%d, 실제=%d", failureCount.get(), couponUnavailableExceptions));
        }

        @DisplayName("성공 케이스: 쿠폰 사용이 원자적으로 처리되어 Lost Update가 발생하지 않음")
        @Test
        void useCoupon_atomicOperation_noLostUpdate() throws InterruptedException {
            // arrange
            int threadCount = 20;
            int couponCount = 20; // 각 스레드가 다른 쿠폰을 사용

            // 여러 개의 쿠폰 생성
            List<Long> couponIds = new ArrayList<>();
            for (int i = 0; i < couponCount; i++) {
                Coupon coupon = Coupon.builder()
                        .couponType(CouponType.FIXED_AMOUNT)
                        .discountValue(BigDecimal.valueOf(5000))
                        .user(test34)
                        .build();
                Coupon savedCoupon = couponRepository.save(coupon)
                        .orElseThrow(() -> new RuntimeException("Coupon 저장 실패"));
                couponIds.add(savedCoupon.getId());
            }

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);

            // act
            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                executor.submit(() -> {
                    try {
                        Order order = createTestOrder();
                        Long couponId = couponIds.get(index % couponCount);
                        couponService.useCoupon(order, couponId);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        // 실패는 무시 (쿠폰 사용 불가 등)
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // wait for all threads to complete
            assertTrue(latch.await(10, TimeUnit.SECONDS), "모든 스레드가 10초 내에 완료되지 않았습니다.");
            executor.shutdown();

            // assert
            // 사용된 쿠폰 수 확인
            long usedCouponCount = couponIds.stream()
                    .mapToLong(couponId -> {
                        Coupon coupon = couponRepository.findById(couponId)
                                .orElseThrow(() -> new RuntimeException("쿠폰을 찾을 수 없습니다"));
                        return coupon.getIsUsed() ? 1 : 0;
                    })
                    .sum();

            // Lost Update가 발생하지 않았는지 확인
            // 성공한 쿠폰 적용 수만큼 정확히 쿠폰이 사용되어야 함
            assertEquals(successCount.get(), usedCouponCount,
                    String.format("Lost Update가 발생하지 않아야 함: 성공한 쿠폰 적용 수=%d, 사용된 쿠폰 수=%d", successCount.get(), usedCouponCount));
        }

        @DisplayName("성공 케이스: 동시에 여러 스레드가 쿠폰을 사용해도 중복 사용되지 않음")
        @Test
        void useCoupon_concurrentRequests_noDuplicateUsage() throws InterruptedException {
            // arrange
            int threadCount = 25; // 쿠폰 1개에 대해 25개 스레드가 동시에 사용 시도

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);

            // act
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        Order order = createTestOrder();
                        couponService.useCoupon(order, testCouponId);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // wait for all threads to complete
            assertTrue(latch.await(10, TimeUnit.SECONDS), "모든 스레드가 10초 내에 완료되지 않았습니다.");
            executor.shutdown();

            // assert
            Coupon finalCoupon = couponRepository.findById(testCouponId)
                    .orElseThrow(() -> new RuntimeException("쿠폰을 찾을 수 없습니다"));

            // 쿠폰이 중복 사용되지 않아야 함 (정확히 1번만 사용)
            assertEquals(1, successCount.get(),
                    String.format("성공한 쿠폰 사용 수는 1이어야 함: 실제=%d", successCount.get()));
            assertEquals(threadCount - 1, failureCount.get(),
                    String.format("실패한 쿠폰 사용 수는 %d이어야 함: 실제=%d", threadCount - 1, failureCount.get()));
            assertTrue(finalCoupon.getIsUsed(),
                    String.format("쿠폰은 사용된 상태여야 함: 실제=%s", finalCoupon.getIsUsed()));
            // 성공한 쿠폰 사용 수만큼 정확히 쿠폰이 사용되어야 함
            assertTrue(finalCoupon.getIsUsed() && successCount.get() == 1,
                    String.format("쿠폰이 정확히 1번만 사용되어야 함: 성공한 사용 수=%d, 쿠폰 사용 상태=%s", successCount.get(), finalCoupon.getIsUsed()));
        }
    }

    private Order createTestOrder() {
        // 테스트용 Order 생성 (각 스레드마다 새로운 Order)
        Brand brand = Brand.builder()
                .name("Test Brand")
                .description("Test Description")
                .status(BrandStatus.ON_SALE)
                .isVisible(true)
                .isSellable(true)
                .build();
        Brand savedBrand = brandJpaRepository.save(brand);

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

        Order order = Order.builder()
                .discountAmount(BigDecimal.ZERO)
                .shippingFee(BigDecimal.ZERO)
                .user(test34)
                .build();
        
        // OrderItem 추가
        order.addOrderItem(savedProduct, 10);

        return orderService.saveOrder(order)
                .orElseThrow(() -> new RuntimeException("Order 저장 실패"));
    }
}

