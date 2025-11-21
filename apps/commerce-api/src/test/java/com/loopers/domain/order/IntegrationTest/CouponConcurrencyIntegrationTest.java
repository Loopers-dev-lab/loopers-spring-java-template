package com.loopers.domain.order.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.loopers.application.order.OrderCreateCommand;
import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderInfo;
import com.loopers.application.order.OrderItemCommand;
import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.application.user.UserRegisterCommand;
import com.loopers.domain.brand.BrandEntity;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.coupon.CouponEntity;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.coupon.CouponStatus;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.ProductDomainCreateRequest;
import com.loopers.domain.product.ProductEntity;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserEntity;
import com.loopers.domain.user.UserService;
import com.loopers.fixtures.BrandTestFixture;
import com.loopers.fixtures.ProductTestFixture;
import com.loopers.fixtures.UserTestFixture;
import com.loopers.utils.DatabaseCleanUp;

/**
 * 쿠폰 동시성 제어 통합 테스트
 *
 * @author hyunjikoh
 * @since 2025. 11. 20.
 */
@SpringBootTest
@DisplayName("쿠폰 동시성 제어 통합 테스트")
public class CouponConcurrencyIntegrationTest {

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private UserFacade userFacade;

    @Autowired
    private ProductService productService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private PointService pointService;

    @Autowired
    private CouponService couponService;

    @Autowired
    private UserService userService;

    @BeforeEach
    void setUp() {
        // 테스트 데이터는 각 테스트에서 필요한 만큼만 생성
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Nested
    @DisplayName("동시 쿠폰 사용")
    class ConcurrentCouponUsage {

        @Test
        @DisplayName("동일한 쿠폰을 동시에 사용하려고 하면 한 번만 성공한다")
        void should_allow_only_one_order_when_using_same_coupon_concurrently() throws InterruptedException {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 두 명의 사용자 생성 및 포인트 충전
            List<UserInfo> users = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                UserRegisterCommand userCommand = new UserRegisterCommand(
                        "user" + i,
                        "user" + i + "@test.com",
                        LocalDate.of(1990, 1, 1).toString(),
                        Gender.MALE
                );
                UserInfo userInfo = userFacade.registerUser(userCommand);
                pointService.charge(userInfo.username(), new BigDecimal("50000"));
                users.add(userInfo);
            }

            // Given: 상품 생성 (충분한 재고)
            ProductDomainCreateRequest productRequest = ProductTestFixture.createRequest(
                    brand.getId(),
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity product = productService.registerProduct(productRequest);

            // Given: 동일한 쿠폰을 첫 번째 사용자에게 발급
            UserEntity user1 = userService.getUserByUsername(users.get(0).username());
            CouponEntity sharedCoupon = couponService.createFixedAmountCoupon(user1, new BigDecimal("5000"));

            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(2);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);
            List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
            List<OrderInfo> successfulOrders = Collections.synchronizedList(new ArrayList<>());

            // When - 두 사용자가 동시에 같은 쿠폰으로 주문 시도
            ExecutorService executor = Executors.newFixedThreadPool(2);

            // 사용자1 주문 스레드
            executor.submit(() -> {
                try {
                    startLatch.await(); // 동시 시작을 위한 대기

                    OrderCreateCommand orderCommand = OrderCreateCommand.builder()
                            .username(users.get(0).username())
                            .orderItems(List.of(
                                    OrderItemCommand.builder()
                                            .productId(product.getId())
                                            .quantity(1)
                                            .couponId(sharedCoupon.getId())
                                            .build()
                            ))
                            .build();

                    OrderInfo result = orderFacade.createOrder(orderCommand);
                    successfulOrders.add(result);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    exceptions.add(e);
                } finally {
                    endLatch.countDown();
                }
            });

            // 사용자1이 동일 쿠폰으로 두 번째 주문 시도
            executor.submit(() -> {
                try {
                    startLatch.await(); // 동시 시작을 위한 대기

                    OrderCreateCommand orderCommand = OrderCreateCommand.builder()
                            .username(users.get(0).username())
                            .orderItems(List.of(
                                    OrderItemCommand.builder()
                                            .productId(product.getId())
                                            .quantity(1)
                                            .couponId(sharedCoupon.getId())
                                            .build()
                            ))
                            .build();

                    OrderInfo result = orderFacade.createOrder(orderCommand);
                    successfulOrders.add(result);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    exceptions.add(e);
                } finally {
                    endLatch.countDown();
                }
            });

            // 동시 실행 시작
            startLatch.countDown();

            // 모든 스레드 완료 대기 (최대 10초)
            boolean completed = endLatch.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            // Then
            assertThat(completed).isTrue(); // 타임아웃 없이 완료되어야 함
            assertThat(successCount.get()).isEqualTo(1); // 정확히 하나만 성공
            assertThat(failureCount.get()).isEqualTo(1); // 정확히 하나만 실패

            // Then: 실패한 경우의 예외 검증 - 낙관적 락 예외 또는 비즈니스 예외
            assertThat(exceptions).hasSize(1);
            Exception failedException = exceptions.get(0);

            // 낙관적 락 예외 또는 비즈니스 예외 중 하나여야 함
            assertThat(failedException)
                    .satisfiesAnyOf(
                            // 낙관적 락 충돌 예외
                            ex -> assertThat(ex)
                                    .isInstanceOf(ObjectOptimisticLockingFailureException.class),
                            // 비즈니스 예외 (쿠폰 이미 사용됨)
                            ex -> assertThat(ex)
                                    .isInstanceOf(IllegalArgumentException.class)
                                    .hasMessageContaining("이미 사용된 쿠폰입니다")
                    );

            // Then: 쿠폰 상태 검증 - 사용됨 상태여야 함
            CouponEntity usedCoupon = couponService.getCouponByIdAndUserId(sharedCoupon.getId(), user1.getId());
            assertThat(usedCoupon.getStatus()).isEqualTo(CouponStatus.USED);

            // Then: 성공한 주문이 정확히 하나만 존재해야 함
            assertThat(successfulOrders).hasSize(1);
            assertThat(successfulOrders.get(0).finalTotalAmount())
                    .isEqualByComparingTo(new BigDecimal("5000")); // 10,000 - 5,000
        }

        @Test
        @DisplayName("낙관적 락 충돌 시 적절한 예외가 발생한다")
        void should_throw_optimistic_lock_exception_on_version_conflict() throws InterruptedException {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 사용자 생성 및 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            pointService.charge(userInfo.username(), new BigDecimal("50000"));

            // Given: 상품 생성
            ProductDomainCreateRequest productRequest = ProductTestFixture.createRequest(
                    brand.getId(),
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity product = productService.registerProduct(productRequest);

            // Given: 쿠폰 생성
            UserEntity user = userService.getUserByUsername(userInfo.username());
            CouponEntity coupon = couponService.createFixedAmountCoupon(user, new BigDecimal("3000"));

            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(2);
            List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
            AtomicInteger successCount = new AtomicInteger(0);

            ExecutorService executor = Executors.newFixedThreadPool(2);

            // When - 동시에 같은 쿠폰 사용 시도
            for (int i = 0; i < 2; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();

                        OrderCreateCommand orderCommand = OrderCreateCommand.builder()
                                .username(userInfo.username())
                                .orderItems(List.of(
                                        OrderItemCommand.builder()
                                                .productId(product.getId())
                                                .quantity(1)
                                                .couponId(coupon.getId())
                                                .build()
                                ))
                                .build();

                        orderFacade.createOrder(orderCommand);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        exceptions.add(e);
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            boolean completed = endLatch.await(5, TimeUnit.SECONDS);
            executor.shutdown();

            // Then
            assertThat(completed).isTrue();
            assertThat(successCount.get()).isEqualTo(1);
            assertThat(exceptions).hasSize(1);

            // Then: 낙관적 락 예외 또는 비즈니스 예외 발생 확인
            Exception exception = exceptions.get(0);
            assertThat(exception)
                    .satisfiesAnyOf(
                            ex -> assertThat(ex).isInstanceOf(ObjectOptimisticLockingFailureException.class),
                            ex -> assertThat(ex).isInstanceOf(IllegalArgumentException.class)
                    );
        }

        @Test
        @DisplayName("쿠폰 사용 시 낙관적 락이 정상적으로 동작한다")
        void should_apply_optimistic_lock_when_using_coupon() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 사용자 생성 및 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            pointService.charge(userInfo.username(), new BigDecimal("20000"));

            // Given: 상품 생성
            ProductDomainCreateRequest productRequest = ProductTestFixture.createRequest(
                    brand.getId(),
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity product = productService.registerProduct(productRequest);

            // Given: 쿠폰 생성
            UserEntity user = userService.getUserByUsername(userInfo.username());
            CouponEntity coupon = couponService.createFixedAmountCoupon(user, new BigDecimal("3000"));

            // When - 정상적인 쿠폰 사용
            OrderCreateCommand orderCommand = OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(List.of(
                            OrderItemCommand.builder()
                                    .productId(product.getId())
                                    .quantity(1)
                                    .couponId(coupon.getId())
                                    .build()
                    ))
                    .build();

            OrderInfo result = orderFacade.createOrder(orderCommand);

            // Then: 주문이 정상적으로 생성되어야 함
            assertThat(result).isNotNull();
            assertThat(result.finalTotalAmount()).isEqualByComparingTo(new BigDecimal("7000")); // 10,000 - 3,000

            // Then: 쿠폰이 사용됨 상태로 변경되어야 함
            CouponEntity usedCoupon = couponService.getCouponByIdAndUserId(coupon.getId(), user.getId());
            assertThat(usedCoupon.getStatus()).isEqualTo(CouponStatus.USED);
            assertThat(usedCoupon.getVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("동일 쿠폰 동시 사용 시 한 트랜잭션만 성공하고 나머지는 실패한다")
        void should_succeed_only_one_transaction_when_concurrent_same_coupon_usage() throws InterruptedException {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 상품 생성 (충분한 재고)
            ProductDomainCreateRequest productRequest = ProductTestFixture.createRequest(
                    brand.getId(),
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity product = productService.registerProduct(productRequest);

            int threadCount = 5; // 5개 스레드로 동시 접근

            // Given: 한 명의 사용자 생성 및 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            pointService.charge(userInfo.username(), new BigDecimal("100000")); // 충분한 포인트

            // Given: 쿠폰 생성 (1회만 사용 가능)
            UserEntity user = userService.getUserByUsername(userInfo.username());
            CouponEntity coupon = couponService.createFixedAmountCoupon(user, new BigDecimal("2000"));

            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);
            List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            // When - 동일한 사용자가 같은 쿠폰으로 여러 번 동시 주문 시도
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();

                        OrderCreateCommand orderCommand = OrderCreateCommand.builder()
                                .username(userInfo.username())
                                .orderItems(List.of(
                                        OrderItemCommand.builder()
                                                .productId(product.getId())
                                                .quantity(1)
                                                .couponId(coupon.getId())
                                                .build()
                                ))
                                .build();

                        orderFacade.createOrder(orderCommand);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                        exceptions.add(e);
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown(); // 동시 실행 시작
            boolean completed = endLatch.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            // Then
            assertThat(completed).isTrue();
            assertThat(successCount.get()).isEqualTo(1); // 정확히 하나만 성공
            assertThat(failureCount.get()).isEqualTo(threadCount - 1); // 나머지는 모두 실패

            // Then: 실패 예외들 검증
            assertThat(exceptions).hasSize(threadCount - 1);
            exceptions.forEach(exception -> {
                assertThat(exception)
                        .satisfiesAnyOf(
                                // 낙관적 락 충돌 예외
                                ex -> assertThat(ex)
                                        .isInstanceOf(ObjectOptimisticLockingFailureException.class),
                                // 비즈니스 예외 (쿠폰 이미 사용됨)
                                ex -> assertThat(ex)
                                        .isInstanceOf(IllegalArgumentException.class)
                                        .hasMessageContaining("이미 사용된 쿠폰입니다")
                        );
            });

            // Then: 쿠폰 상태 검증
            CouponEntity usedCoupon = couponService.getCouponByIdAndUserId(coupon.getId(), user.getId());
            assertThat(usedCoupon.getStatus()).isEqualTo(CouponStatus.USED);
        }

        @Test
        @DisplayName("동시 쿠폰 사용 실패 시 적절한 예외 메시지가 반환된다")
        void should_return_appropriate_error_message_when_concurrent_coupon_usage_fails() throws InterruptedException {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 사용자 생성 및 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            pointService.charge(userInfo.username(), new BigDecimal("50000"));

            // Given: 상품 생성
            ProductDomainCreateRequest productRequest = ProductTestFixture.createRequest(
                    brand.getId(),
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity product = productService.registerProduct(productRequest);

            // Given: 쿠폰 생성
            UserEntity user = userService.getUserByUsername(userInfo.username());
            CouponEntity coupon = couponService.createFixedAmountCoupon(user, new BigDecimal("1000"));

            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(2);
            List<String> errorMessages = Collections.synchronizedList(new ArrayList<>());
            List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
            AtomicInteger successCount = new AtomicInteger(0);

            ExecutorService executor = Executors.newFixedThreadPool(2);

            // When - 동시 쿠폰 사용 시도
            for (int i = 0; i < 2; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();

                        OrderCreateCommand orderCommand = OrderCreateCommand.builder()
                                .username(userInfo.username())
                                .orderItems(List.of(
                                        OrderItemCommand.builder()
                                                .productId(product.getId())
                                                .quantity(1)
                                                .couponId(coupon.getId())
                                                .build()
                                ))
                                .build();

                        orderFacade.createOrder(orderCommand);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        errorMessages.add(e.getMessage());
                        exceptions.add(e);
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            boolean completed = endLatch.await(5, TimeUnit.SECONDS);
            executor.shutdown();

            // Then
            assertThat(completed).isTrue();
            assertThat(successCount.get()).isEqualTo(1);
            assertThat(errorMessages).hasSize(1);
            assertThat(exceptions).hasSize(1);

            // Then: 예외 타입 검증 - 낙관적 락 예외 또는 비즈니스 예외
            Exception failedException = exceptions.get(0);
            assertThat(failedException)
                    .satisfiesAnyOf(
                            // 낙관적 락 충돌 예외
                            ex -> assertThat(ex).isInstanceOf(ObjectOptimisticLockingFailureException.class),
                            // 비즈니스 예외 (쿠폰 이미 사용됨)
                            ex -> assertThat(ex).isInstanceOf(IllegalArgumentException.class)
                    );

            // Then: 에러 메시지가 사용자 친화적이어야 함 (비즈니스 예외인 경우)
            if (failedException instanceof IllegalArgumentException) {
                String errorMessage = errorMessages.get(0);
                assertThat(errorMessage)
                        .satisfiesAnyOf(
                                msg -> assertThat(msg).contains("이미 사용된 쿠폰"),
                                msg -> assertThat(msg).contains("쿠폰을 사용할 수 없습니다"),
                                msg -> assertThat(msg).contains("사용된 쿠폰입니다")
                        );
            }
        }
    }

    @Nested
    @DisplayName("서로 다른 쿠폰 동시 사용")
    class DifferentCouponsConcurrentUsage {

        @Test
        @DisplayName("여러 사용자가 각자의 쿠폰을 동시에 사용할 수 있다")
        void should_allow_concurrent_orders_with_different_coupons() throws InterruptedException {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 상품 생성 (충분한 재고)
            ProductDomainCreateRequest productRequest = ProductTestFixture.createRequest(
                    brand.getId(),
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity product = productService.registerProduct(productRequest);

            int userCount = 3;
            List<UserInfo> users = new ArrayList<>();
            List<CouponEntity> coupons = new ArrayList<>();

            // Given: 각 사용자마다 서로 다른 쿠폰 생성 및 발급
            for (int i = 0; i < userCount; i++) {
                // 사용자 생성
                UserRegisterCommand userCommand = new UserRegisterCommand(
                        "user" + i,
                        "user" + i + "@test.com",
                        LocalDate.of(1990, 1, 1).toString(),
                        Gender.MALE
                );
                UserInfo userInfo = userFacade.registerUser(userCommand);
                pointService.charge(userInfo.username(), new BigDecimal("50000"));
                users.add(userInfo);

                // 각 사용자에게 서로 다른 할인 금액의 쿠폰 발급
                UserEntity userEntity = userService.getUserByUsername(userInfo.username());
                BigDecimal discountAmount = new BigDecimal(1000 * (i + 1)); // 1000, 2000, 3000
                CouponEntity coupon = couponService.createFixedAmountCoupon(userEntity, discountAmount);
                coupons.add(coupon);
            }

            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(userCount);
            AtomicInteger successCount = new AtomicInteger(0);
            List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
            List<OrderInfo> createdOrders = Collections.synchronizedList(new ArrayList<>());

            ExecutorService executor = Executors.newFixedThreadPool(userCount);

            // When - 각 사용자가 자신의 쿠폰으로 동시 주문
            for (int i = 0; i < userCount; i++) {
                final int index = i;
                executor.submit(() -> {
                    try {
                        startLatch.await();

                        OrderCreateCommand orderCommand = OrderCreateCommand.builder()
                                .username(users.get(index).username())
                                .orderItems(List.of(
                                        OrderItemCommand.builder()
                                                .productId(product.getId())
                                                .quantity(1)
                                                .couponId(coupons.get(index).getId())
                                                .build()
                                ))
                                .build();

                        OrderInfo result = orderFacade.createOrder(orderCommand);
                        createdOrders.add(result);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        exceptions.add(e);
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            boolean completed = endLatch.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            // Then
            assertThat(completed).isTrue();
            assertThat(successCount.get()).isEqualTo(userCount); // 모든 주문이 성공해야 함
            assertThat(exceptions).isEmpty(); // 예외가 발생하지 않아야 함
            assertThat(createdOrders).hasSize(userCount);

            // Then: 모든 쿠폰이 사용됨 상태여야 함
            for (int i = 0; i < userCount; i++) {
                UserEntity user = userService.getUserByUsername(users.get(i).username());
                CouponEntity coupon = couponService.getCouponByIdAndUserId(coupons.get(i).getId(), user.getId());
                assertThat(coupon.getStatus()).isEqualTo(CouponStatus.USED);
            }

            // Then: 각 주문의 할인 금액 검증
            for (int i = 0; i < userCount; i++) {
                BigDecimal expectedDiscount = new BigDecimal(1000 * (i + 1));
                BigDecimal expectedFinalAmount = new BigDecimal("10000").subtract(expectedDiscount);

                boolean hasMatchingOrder = createdOrders.stream()
                        .anyMatch(order -> order.finalTotalAmount().compareTo(expectedFinalAmount) == 0);
                assertThat(hasMatchingOrder)
                        .as("할인 금액 %s원이 적용된 주문이 존재해야 함", expectedDiscount)
                        .isTrue();
            }
        }
    }
}
