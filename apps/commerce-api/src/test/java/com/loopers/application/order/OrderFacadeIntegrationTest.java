package com.loopers.application.order;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.common.vo.Price;
import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.metrics.product.ProductMetrics;
import com.loopers.domain.point.Point;
import com.loopers.domain.product.Product;
import com.loopers.domain.supply.Supply;
import com.loopers.domain.supply.vo.Stock;
import com.loopers.domain.user.User;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.coupon.CouponJpaRepository;
import com.loopers.infrastructure.metrics.product.ProductMetricsJpaRepository;
import com.loopers.infrastructure.point.PointJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.infrastructure.supply.SupplyJpaRepository;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@DisplayName("주문 Facade(OrderFacade) 통합 테스트")
public class OrderFacadeIntegrationTest {
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private PointJpaRepository pointJpaRepository;

    @Autowired
    private BrandJpaRepository brandJpaRepository;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private ProductMetricsJpaRepository productMetricsJpaRepository;

    @Autowired
    private SupplyJpaRepository supplyJpaRepository;

    @Autowired
    private CouponJpaRepository couponJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private String userId;
    private Long userEntityId;
    private Long brandId;
    private Long productId1;
    private Long productId2;
    private Long productId3;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @BeforeEach
    void setup() {
        // User 등록
        User user = User.create("user123", "test@test.com", "1993-03-13", "male");
        User savedUser = userJpaRepository.save(user);
        userId = savedUser.getUserId();
        userEntityId = savedUser.getId();

        // Point 등록 및 충전
        // 초기 포인트: 100000원
        Point point = Point.create(userEntityId);
        point.charge(100000);
        pointJpaRepository.save(point);

        // Brand 등록
        Brand brand = Brand.create("Nike");
        Brand savedBrand = brandJpaRepository.save(brand);
        brandId = savedBrand.getId();

        // Product 등록
        Product product1 = Product.create("상품1", brandId, new Price(10000));
        Product savedProduct1 = productJpaRepository.save(product1);
        productId1 = savedProduct1.getId();
        ProductMetrics metrics1 = ProductMetrics.create(productId1, brandId, 0);
        productMetricsJpaRepository.save(metrics1);

        Product product2 = Product.create("상품2", brandId, new Price(20000));
        Product savedProduct2 = productJpaRepository.save(product2);
        productId2 = savedProduct2.getId();
        ProductMetrics metrics2 = ProductMetrics.create(productId2, brandId, 0);
        productMetricsJpaRepository.save(metrics2);

        Product product3 = Product.create("상품3", brandId, new Price(15000));
        Product savedProduct3 = productJpaRepository.save(product3);
        productId3 = savedProduct3.getId();
        ProductMetrics metrics3 = ProductMetrics.create(productId3, brandId, 0);
        productMetricsJpaRepository.save(metrics3);

        // Supply 등록 (재고 설정)
        Supply supply1 = Supply.create(productId1, new Stock(100));
        supplyJpaRepository.save(supply1);

        Supply supply2 = Supply.create(productId2, new Stock(50));
        supplyJpaRepository.save(supply2);

        Supply supply3 = Supply.create(productId3, new Stock(5)); // 재고 부족 테스트를 위해 재고를 적게 설정
        supplyJpaRepository.save(supply3);
    }

    @DisplayName("주문 생성 시, ")
    @Nested
    class CreateOrder {
        @DisplayName("정상적인 주문을 생성할 수 있다. (Happy Path)")
        @Test
        void should_createOrder_when_validRequest() {
            // arrange
            // 초기 포인트: 100000원 (setup에서 설정)
            // 상품1: 10000원 * 2개 = 20000원
            // 상품2: 20000원 * 1개 = 20000원
            // 총 주문 금액: 40000원
            // 예상 남은 포인트: 100000 - 40000 = 60000원
            OrderRequest request = new OrderRequest(
                    List.of(
                            new OrderItemRequest(productId1, 2),
                            new OrderItemRequest(productId2, 1)
                    )
            );

            // act
            OrderInfo orderInfo = orderFacade.createOrder(userId, request);

            // assert
            assertThat(orderInfo).isNotNull();
            assertThat(orderInfo.orderId()).isNotNull();
            assertThat(orderInfo.items()).hasSize(2);
            assertThat(orderInfo.totalPrice()).isEqualTo(40000);

            // assert - 포인트 차감 확인
            Point point = pointJpaRepository.findByUserId(userEntityId).orElseThrow();
            assertThat(point.getAmount()).isEqualTo(60000); // 100000 - 40000
        }

        @DisplayName("동시에 주문해도 재고가 정상적으로 차감된다.")
        @Test
        void concurrencyTest_stockShouldBeProperlyDecreasedWhenOrdersCreated() throws InterruptedException {
            // arrange
            // 초기 포인트: 100000원 (setup에서 설정)
            // 초기 재고: productId1 = 100개
            // 주문: 상품1 1개씩 10건 동시 주문
            // 각 주문 금액: 10000원
            // 총 주문 금액: 10000원 * 10건 = 100000원
            // 예상 남은 포인트: 100000 - 100000 = 0원
            OrderRequest request = new OrderRequest(
                    List.of(
                            new OrderItemRequest(productId1, 1) // 재고 100, 가격 10000원
                    )
            );
            int threadCount = 10;
            CountDownLatch latch = new CountDownLatch(threadCount);

            // act
            try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
                for (int i = 0; i < threadCount; i++) {
                    executor.submit(() -> {
                        TransactionTemplate template = new TransactionTemplate(transactionManager);
                        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);  // 새 트랜잭션 강제
                        template.execute(status -> {
                            try {
                                orderFacade.createOrder(userId, request);
                            } catch (Exception e) {
                                System.out.println("실패: " + e.getMessage());
                                status.setRollbackOnly();  // 예외 시 명시 롤백
                            }
                            return null;
                        });
                        latch.countDown();
                    });
                }
            }

            latch.await();

            // assert - 재고 차감 확인
            Supply supply = supplyJpaRepository.findByProductId(productId1).orElseThrow();
            assertThat(supply.getStock().quantity()).isEqualTo(90); // 100 - 10

            // assert - 포인트 차감 확인
            Point point = pointJpaRepository.findByUserId(userEntityId).orElseThrow();
            assertThat(point.getAmount()).isEqualTo(0); // 100000 - (10000 * 10) = 0
        }

        @DisplayName("존재하지 않는 상품 ID가 포함된 경우, 예외가 발생한다. (Exception)")
        @Test
        void should_throwException_when_productIdDoesNotExist() {
            // arrange
            Long nonExistentProductId = 99999L;
            OrderRequest request = new OrderRequest(
                    List.of(
                            new OrderItemRequest(nonExistentProductId, 1)
                    )
            );

            // act & assert
            // Note: 현재 구현에서는 productMap.get()이 null을 반환하여 NullPointerException이 발생할 수 있음
            // 또는 SupplyService.checkAndDecreaseStock에서 NOT_FOUND 예외가 발생할 수 있음
            assertThrows(Exception.class, () -> orderFacade.createOrder(userId, request));
        }

        @DisplayName("단일 상품 재고 부족 시, 예외가 발생한다. (Exception)")
        @Test
        void should_throwException_when_singleProductStockInsufficient() {
            // arrange
            OrderRequest request = new OrderRequest(
                    List.of(
                            new OrderItemRequest(productId1, 99999)
                    )
            );

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> orderFacade.createOrder(userId, request));
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("재고가 부족합니다");
        }

        @DisplayName("여러 상품 중 일부만 재고 부족 시, 예외가 발생한다. (Exception)")
        @Test
        void should_throwException_when_partialStockInsufficient() {
            // arrange
            // productId1: 재고 100, productId2: 재고 50
            // productId1은 충분하지만 productId2는 부족
            OrderRequest request = new OrderRequest(
                    List.of(
                            new OrderItemRequest(productId1, 10), // 재고 충분
                            new OrderItemRequest(productId2, 99999) // 재고 부족
                    )
            );

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> orderFacade.createOrder(userId, request));
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("재고가 부족합니다");
        }

        @DisplayName("여러 상품 모두 재고 부족 시, 예외가 발생한다. (Exception)")
        @Test
        void should_throwException_when_allProductsStockInsufficient() {
            // arrange
            OrderRequest request = new OrderRequest(
                    List.of(
                            new OrderItemRequest(productId1, 99999),
                            new OrderItemRequest(productId2, 99999)
                    )
            );

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> orderFacade.createOrder(userId, request));
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("재고가 부족합니다");
        }

        @DisplayName("포인트 부족 시, 예외가 발생한다. (Exception)")
        @Test
        void should_throwException_when_pointInsufficient() {
            // arrange
            // 초기 포인트: 100000원 (setup에서 설정)
            // 첫 번째 주문: 상품1 10개 = 100000원
            // 첫 번째 주문 후 포인트: 100000 - 100000 = 0원
            OrderRequest firstOrder = new OrderRequest(
                    List.of(
                            new OrderItemRequest(productId1, 10) // 100000원 사용
                    )
            );
            orderFacade.createOrder(userId, firstOrder);
            // 남은 포인트: 0원

            // 두 번째 주문 시도: 상품2 1개 = 20000원
            // 포인트 부족(0원 < 20000원) → 예외 발생
            OrderRequest request = new OrderRequest(
                    List.of(
                            new OrderItemRequest(productId2, 1) // 20000원 필요 (부족)
                    )
            );

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> orderFacade.createOrder(userId, request));
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("포인트가 부족합니다");
        }

        @DisplayName("포인트가 정확히 일치할 경우, 주문이 성공한다. (Edge Case)")
        @Test
        void should_createOrder_when_pointExactlyMatches() {
            // arrange
            // 초기 포인트: 100000원 (setup에서 설정)
            // 첫 번째 주문: 상품1 9개 = 90000원
            // 첫 번째 주문 후 포인트: 100000 - 90000 = 10000원
            OrderRequest firstOrder = new OrderRequest(
                    List.of(
                            new OrderItemRequest(productId1, 9) // 90000원 사용
                    )
            );
            orderFacade.createOrder(userId, firstOrder);
            // 남은 포인트: 10000원

            // 두 번째 주문: 상품1 1개 = 10000원 (정확히 일치)
            // 예상 남은 포인트: 10000 - 10000 = 0원
            OrderRequest request = new OrderRequest(
                    List.of(
                            new OrderItemRequest(productId1, 1) // 정확히 10000원
                    )
            );

            // act
            OrderInfo orderInfo = orderFacade.createOrder(userId, request);

            // assert
            assertThat(orderInfo).isNotNull();
            assertThat(orderInfo.totalPrice()).isEqualTo(10000);

            // assert - 포인트가 정확히 차감되었는지 확인
            Point point = pointJpaRepository.findByUserId(userEntityId).orElseThrow();
            assertThat(point.getAmount()).isEqualTo(0); // 10000 - 10000 = 0
        }

        @DisplayName("중복 상품이 포함된 경우, CoreException가 발생한다. (Exception)")
        @Test
        void should_throwException_when_duplicateProducts() {
            // arrange
            // 같은 상품을 여러 번 주문 항목에 포함
            // Note: Collectors.toMap()은 중복 키가 있으면 CoreException를 발생시킴
            OrderRequest request = new OrderRequest(
                    List.of(
                            new OrderItemRequest(productId1, 2),
                            new OrderItemRequest(productId1, 3) // 중복
                    )
            );

            // act & assert
            assertThrows(CoreException.class, () -> orderFacade.createOrder(userId, request));
        }

        @DisplayName("존재하지 않는 사용자로 주문 시도 시, 예외가 발생한다. (Exception)")
        @Test
        void should_throwException_when_userDoesNotExist() {
            // arrange
            String nonExistentUserId = "nonexist";
            OrderRequest request = new OrderRequest(
                    List.of(
                            new OrderItemRequest(productId1, 1)
                    )
            );

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> orderFacade.createOrder(nonExistentUserId, request));
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
            assertThat(exception.getMessage()).contains("사용자를 찾을 수 없습니다");
        }

        @DisplayName("정액 쿠폰을 적용한 주문이 성공적으로 생성되고, 재고/포인트/쿠폰이 정상적으로 차감된다. (Happy Path)")
        @Test
        void should_createOrderWithFixedCoupon_when_validRequest() {
            // arrange
            // 초기 포인트: 100000원 (setup에서 설정)
            // 상품1: 10000원 * 2개 = 20000원
            // 상품2: 20000원 * 1개 = 20000원
            // 원래 총액: 40000원
            // 정액 쿠폰 할인: 5000원
            // 최종 주문 금액: 40000 - 5000 = 35000원
            // 예상 남은 포인트: 100000 - 35000 = 65000원
            Coupon fixedCoupon = Coupon.issueFixed(userEntityId, 5000);
            Coupon savedCoupon = couponJpaRepository.save(fixedCoupon);
            Long couponId = savedCoupon.getId();

            OrderRequest request = new OrderRequest(
                    List.of(
                            new OrderItemRequest(productId1, 2), // 20000원
                            new OrderItemRequest(productId2, 1)  // 20000원
                    ),
                    couponId // 총액 40000원에서 5000원 할인 = 35000원
            );

            // act
            OrderInfo orderInfo = orderFacade.createOrder(userId, request);

            // assert - 주문 생성 확인
            assertThat(orderInfo).isNotNull();
            assertThat(orderInfo.orderId()).isNotNull();
            assertThat(orderInfo.items()).hasSize(2);
            assertThat(orderInfo.totalPrice()).isEqualTo(35000); // 40000 - 5000

            // assert - 재고 차감 확인
            Supply supply1 = supplyJpaRepository.findByProductId(productId1).orElseThrow();
            assertThat(supply1.getStock().quantity()).isEqualTo(98); // 100 - 2
            Supply supply2 = supplyJpaRepository.findByProductId(productId2).orElseThrow();
            assertThat(supply2.getStock().quantity()).isEqualTo(49); // 50 - 1

            // assert - 포인트 차감 확인
            Point point = pointJpaRepository.findByUserId(userEntityId).orElseThrow();
            assertThat(point.getAmount()).isEqualTo(65000); // 100000 - 35000

            // assert - 쿠폰 사용 확인
            Coupon usedCoupon = couponJpaRepository.findById(couponId).orElseThrow();
            assertThat(usedCoupon.isUsed()).isTrue();
        }

        @DisplayName("정률 쿠폰을 적용한 주문이 성공적으로 생성되고, 재고/포인트/쿠폰이 정상적으로 차감된다. (Happy Path)")
        @Test
        void should_createOrderWithPercentageCoupon_when_validRequest() {
            // arrange
            // 초기 포인트: 100000원 (setup에서 설정)
            // 상품1: 10000원 * 2개 = 20000원
            // 상품2: 20000원 * 1개 = 20000원
            // 원래 총액: 40000원
            // 정률 쿠폰 할인: 20% = 8000원
            // 최종 주문 금액: 40000 - 8000 = 32000원
            // 예상 남은 포인트: 100000 - 32000 = 68000원
            Coupon percentageCoupon = Coupon.issuePercentage(userEntityId, 20); // 20% 할인
            Coupon savedCoupon = couponJpaRepository.save(percentageCoupon);
            Long couponId = savedCoupon.getId();

            OrderRequest request = new OrderRequest(
                    List.of(
                            new OrderItemRequest(productId1, 2), // 20000원
                            new OrderItemRequest(productId2, 1)  // 20000원
                    ),
                    couponId // 총액 40000원에서 20% 할인 = 32000원
            );

            // act
            OrderInfo orderInfo = orderFacade.createOrder(userId, request);

            // assert - 주문 생성 확인
            assertThat(orderInfo).isNotNull();
            assertThat(orderInfo.totalPrice()).isEqualTo(32000); // 40000 - 8000

            // assert - 재고 차감 확인
            Supply supply1 = supplyJpaRepository.findByProductId(productId1).orElseThrow();
            assertThat(supply1.getStock().quantity()).isEqualTo(98); // 100 - 2
            Supply supply2 = supplyJpaRepository.findByProductId(productId2).orElseThrow();
            assertThat(supply2.getStock().quantity()).isEqualTo(49); // 50 - 1

            // assert - 포인트 차감 확인
            Point point = pointJpaRepository.findByUserId(userEntityId).orElseThrow();
            assertThat(point.getAmount()).isEqualTo(68000); // 100000 - 32000

            // assert - 쿠폰 사용 확인
            Coupon usedCoupon = couponJpaRepository.findById(couponId).orElseThrow();
            assertThat(usedCoupon.isUsed()).isTrue();
        }

        @DisplayName("여러 상품을 주문할 때, 각 상품의 재고가 정확히 차감되고 포인트가 총액 기준으로 차감된다.")
        @Test
        void should_deductStockCorrectly_when_multipleProductsOrdered() {
            // 수정: 총액이 100000원 이하가 되도록 조정
            // 상품1: 10000원 * 2개 = 20000원
            // 상품2: 20000원 * 2개 = 40000원
            // 상품3: 15000원 * 2개 = 30000원
            // 총 주문 금액: 20000 + 40000 + 30000 = 90000원
            // 예상 남은 포인트: 100000 - 90000 = 10000원
            OrderRequest request = new OrderRequest(
                    List.of(
                            new OrderItemRequest(productId1, 2), // 20000원
                            new OrderItemRequest(productId2, 2), // 40000원
                            new OrderItemRequest(productId3, 2)  // 30000원
                    )
            ); // 총액: 90000원

            // act
            OrderInfo orderInfo = orderFacade.createOrder(userId, request);

            // assert - 주문 생성 확인
            assertThat(orderInfo).isNotNull();
            assertThat(orderInfo.items()).hasSize(3);
            assertThat(orderInfo.totalPrice()).isEqualTo(90000);

            // assert - 각 상품의 재고가 정확히 차감되었는지 확인
            Supply supply1 = supplyJpaRepository.findByProductId(productId1).orElseThrow();
            assertThat(supply1.getStock().quantity()).isEqualTo(98); // 100 - 2

            Supply supply2 = supplyJpaRepository.findByProductId(productId2).orElseThrow();
            assertThat(supply2.getStock().quantity()).isEqualTo(48); // 50 - 2

            Supply supply3 = supplyJpaRepository.findByProductId(productId3).orElseThrow();
            assertThat(supply3.getStock().quantity()).isEqualTo(3); // 5 - 2

            // assert - 포인트가 총액 기준으로 차감되었는지 확인
            Point point = pointJpaRepository.findByUserId(userEntityId).orElseThrow();
            assertThat(point.getAmount()).isEqualTo(10000); // 100000 - 90000
        }

        @DisplayName("존재하지 않는 쿠폰으로 주문 시도 시, 예외가 발생하고 트랜잭션이 롤백된다.")
        @Test
        void should_rollback_when_couponDoesNotExist() {
            // arrange
            // 초기 포인트: 100000원 (setup에서 설정)
            // 초기 재고: productId1 = 100개, productId2 = 50개
            // 주문 시도: 상품1 2개(20000원) + 상품2 1개(20000원) = 40000원
            // 존재하지 않는 쿠폰 사용 시도 → 예외 발생 → 롤백
            Long nonExistentCouponId = 99999L;
            int initialStock1 = supplyJpaRepository.findByProductId(productId1).orElseThrow().getStock().quantity();
            int initialStock2 = supplyJpaRepository.findByProductId(productId2).orElseThrow().getStock().quantity();
            long initialPoint = pointJpaRepository.findByUserId(userEntityId).orElseThrow().getAmount();

            OrderRequest request = new OrderRequest(
                    List.of(
                            new OrderItemRequest(productId1, 2),
                            new OrderItemRequest(productId2, 1)
                    ),
                    nonExistentCouponId
            );

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> orderFacade.createOrder(userId, request));
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
            assertThat(exception.getMessage()).contains("쿠폰을 찾을 수 없습니다");

            // assert - 트랜잭션 롤백 확인: 재고가 변경되지 않았는지 확인
            Supply supply1 = supplyJpaRepository.findByProductId(productId1).orElseThrow();
            assertThat(supply1.getStock().quantity()).isEqualTo(initialStock1);
            Supply supply2 = supplyJpaRepository.findByProductId(productId2).orElseThrow();
            assertThat(supply2.getStock().quantity()).isEqualTo(initialStock2);

            // assert - 포인트가 차감되지 않았는지 확인
            Point point = pointJpaRepository.findByUserId(userEntityId).orElseThrow();
            assertThat(point.getAmount()).isEqualTo(initialPoint);
        }

        @DisplayName("이미 사용된 쿠폰으로 주문 시도 시, 예외가 발생하고 트랜잭션이 롤백된다.")
        @Test
        void should_rollback_when_couponAlreadyUsed() {
            // arrange
            // 초기 포인트: 100000원 (setup에서 설정)
            // 첫 번째 주문: 상품1 1개(10000원) + 정액 쿠폰 5000원 할인 = 5000원
            // 첫 번째 주문 후 포인트: 100000 - 5000 = 95000원
            Coupon coupon = Coupon.issueFixed(userEntityId, 5000);
            Coupon savedCoupon = couponJpaRepository.save(coupon);
            Long couponId = savedCoupon.getId();

            // 쿠폰을 먼저 사용
            OrderRequest firstOrder = new OrderRequest(
                    List.of(new OrderItemRequest(productId1, 1)), // 10000원, 쿠폰 할인 후 5000원
                    couponId
            );
            orderFacade.createOrder(userId, firstOrder);
            // 첫 번째 주문 후 포인트: 100000 - 5000 = 95000원

            // 두 번째 주문 시도: 상품1 2개(20000원) + 상품2 1개(20000원) = 40000원
            // 이미 사용된 쿠폰 사용 시도 → 예외 발생 → 롤백
            int initialStock1 = supplyJpaRepository.findByProductId(productId1).orElseThrow().getStock().quantity();
            int initialStock2 = supplyJpaRepository.findByProductId(productId2).orElseThrow().getStock().quantity();
            long initialPoint = pointJpaRepository.findByUserId(userEntityId).orElseThrow().getAmount();

            // 이미 사용된 쿠폰으로 다시 주문 시도
            OrderRequest request = new OrderRequest(
                    List.of(
                            new OrderItemRequest(productId1, 2),
                            new OrderItemRequest(productId2, 1)
                    ),
                    couponId
            );

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> orderFacade.createOrder(userId, request));
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("이미 사용된 쿠폰입니다");

            // assert - 트랜잭션 롤백 확인: 재고가 변경되지 않았는지 확인
            Supply supply1 = supplyJpaRepository.findByProductId(productId1).orElseThrow();
            assertThat(supply1.getStock().quantity()).isEqualTo(initialStock1);
            Supply supply2 = supplyJpaRepository.findByProductId(productId2).orElseThrow();
            assertThat(supply2.getStock().quantity()).isEqualTo(initialStock2);

            // assert - 포인트가 차감되지 않았는지 확인
            Point point = pointJpaRepository.findByUserId(userEntityId).orElseThrow();
            assertThat(point.getAmount()).isEqualTo(initialPoint);
        }

        @DisplayName("다른 사용자의 쿠폰으로 주문 시도 시, 예외가 발생하고 트랜잭션이 롤백된다.")
        @Test
        void should_rollback_when_couponBelongsToDifferentUser() {
            // arrange
            // 초기 포인트: 100000원 (setup에서 설정)
            // 주문 시도: 상품1 2개(20000원) + 상품2 1개(20000원) = 40000원
            // 다른 사용자의 쿠폰 사용 시도 → 예외 발생 → 롤백
            // 다른 사용자 생성
            User otherUser = User.create("otherUser", "other@test.com", "1990-01-01", "female");
            User savedOtherUser = userJpaRepository.save(otherUser);
            Long otherUserId = savedOtherUser.getId();

            // 다른 사용자의 쿠폰 생성
            Coupon otherUserCoupon = Coupon.issueFixed(otherUserId, 5000);
            Coupon savedCoupon = couponJpaRepository.save(otherUserCoupon);
            Long couponId = savedCoupon.getId();

            int initialStock1 = supplyJpaRepository.findByProductId(productId1).orElseThrow().getStock().quantity();
            int initialStock2 = supplyJpaRepository.findByProductId(productId2).orElseThrow().getStock().quantity();
            long initialPoint = pointJpaRepository.findByUserId(userEntityId).orElseThrow().getAmount();

            // 다른 사용자의 쿠폰으로 주문 시도
            OrderRequest request = new OrderRequest(
                    List.of(
                            new OrderItemRequest(productId1, 2),
                            new OrderItemRequest(productId2, 1)
                    ),
                    couponId
            );

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> orderFacade.createOrder(userId, request));
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
            assertThat(exception.getMessage()).contains("쿠폰을 찾을 수 없습니다");

            // assert - 트랜잭션 롤백 확인
            Supply supply1 = supplyJpaRepository.findByProductId(productId1).orElseThrow();
            assertThat(supply1.getStock().quantity()).isEqualTo(initialStock1);
            Supply supply2 = supplyJpaRepository.findByProductId(productId2).orElseThrow();
            assertThat(supply2.getStock().quantity()).isEqualTo(initialStock2);

            Point point = pointJpaRepository.findByUserId(userEntityId).orElseThrow();
            assertThat(point.getAmount()).isEqualTo(initialPoint);
        }

        @DisplayName("재고 차감 후 포인트 부족으로 실패 시, 모든 변경사항이 롤백된다.")
        @Test
        void should_rollbackAllChanges_when_pointInsufficientAfterStockDeduction() {
            // arrange
            // 초기 포인트: 100000원 (setup에서 설정)
            // 첫 번째 주문: 상품1 9개 = 90000원
            // 첫 번째 주문 후 포인트: 100000 - 90000 = 10000원
            OrderRequest firstOrder = new OrderRequest(
                    List.of(new OrderItemRequest(productId1, 9)) // 90000원 사용
            );
            orderFacade.createOrder(userId, firstOrder);
            // 남은 포인트: 10000원

            // 두 번째 주문 시도: 상품1 2개(20000원) + 상품2 1개(20000원) = 40000원
            // 포인트 부족(10000원 < 40000원) → 예외 발생 → 롤백
            int initialStock1 = supplyJpaRepository.findByProductId(productId1).orElseThrow().getStock().quantity();
            int initialStock2 = supplyJpaRepository.findByProductId(productId2).orElseThrow().getStock().quantity();
            long initialPoint = pointJpaRepository.findByUserId(userEntityId).orElseThrow().getAmount();

            // 포인트가 부족한 주문 (40000원 필요하지만 10000원만 있음)
            OrderRequest request = new OrderRequest(
                    List.of(
                            new OrderItemRequest(productId1, 2), // 20000원
                            new OrderItemRequest(productId2, 1)  // 20000원
                    )
            );

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> orderFacade.createOrder(userId, request));
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("포인트가 부족합니다");

            // assert - 트랜잭션 롤백 확인: 재고가 변경되지 않았는지 확인
            Supply supply1 = supplyJpaRepository.findByProductId(productId1).orElseThrow();
            assertThat(supply1.getStock().quantity()).isEqualTo(initialStock1);
            Supply supply2 = supplyJpaRepository.findByProductId(productId2).orElseThrow();
            assertThat(supply2.getStock().quantity()).isEqualTo(initialStock2);

            // assert - 포인트가 차감되지 않았는지 확인
            Point pointAfter = pointJpaRepository.findByUserId(userEntityId).orElseThrow();
            assertThat(pointAfter.getAmount()).isEqualTo(initialPoint);
        }

        @DisplayName("포인트 차감 후 쿠폰 적용 실패 시, 모든 변경사항이 롤백된다.")
        @Test
        void should_rollbackAllChanges_when_couponFailsAfterPointDeduction() {
            // arrange
            // 초기 포인트: 100000원 (setup에서 설정)
            // 주문 시도: 상품1 2개(20000원) + 상품2 1개(20000원) = 40000원
            // 쿠폰 할인 시도: 5000원 할인 → 최종 금액 35000원
            // 하지만 이미 사용된 쿠폰 → 예외 발생 → 롤백
            // 이미 사용된 쿠폰 생성
            Coupon usedCoupon = Coupon.issueFixed(userEntityId, 5000);
            usedCoupon.applyDiscount(new Price(10000)); // 쿠폰 사용
            Coupon savedCoupon = couponJpaRepository.save(usedCoupon);
            Long couponId = savedCoupon.getId();

            int initialStock1 = supplyJpaRepository.findByProductId(productId1).orElseThrow().getStock().quantity();
            int initialStock2 = supplyJpaRepository.findByProductId(productId2).orElseThrow().getStock().quantity();
            long initialPoint = pointJpaRepository.findByUserId(userEntityId).orElseThrow().getAmount();

            // 이미 사용된 쿠폰으로 주문 시도
            OrderRequest request = new OrderRequest(
                    List.of(
                            new OrderItemRequest(productId1, 2),
                            new OrderItemRequest(productId2, 1)
                    ),
                    couponId
            );

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> orderFacade.createOrder(userId, request));
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("이미 사용된 쿠폰입니다");

            // assert - 트랜잭션 롤백 확인: 재고가 변경되지 않았는지 확인
            Supply supply1 = supplyJpaRepository.findByProductId(productId1).orElseThrow();
            assertThat(supply1.getStock().quantity()).isEqualTo(initialStock1);
            Supply supply2 = supplyJpaRepository.findByProductId(productId2).orElseThrow();
            assertThat(supply2.getStock().quantity()).isEqualTo(initialStock2);

            // assert - 포인트가 차감되지 않았는지 확인
            Point point = pointJpaRepository.findByUserId(userEntityId).orElseThrow();
            assertThat(point.getAmount()).isEqualTo(initialPoint);
        }

        @DisplayName("상품3의 재고가 부족할 때, 예외가 발생하고 트랜잭션이 롤백된다.")
        @Test
        void should_rollback_when_product3StockInsufficient() {
            // arrange
            // 초기 포인트: 100000원 (setup에서 설정)
            // 초기 재고: productId3 = 5개
            // 주문 시도: 상품3 6개(90000원) - 재고 부족
            // 재고 부족으로 예외 발생 → 롤백
            int initialStock3 = supplyJpaRepository.findByProductId(productId3).orElseThrow().getStock().quantity();
            long initialPoint = pointJpaRepository.findByUserId(userEntityId).orElseThrow().getAmount();

            // 재고가 부족한 주문 (상품3 재고 5개인데 6개 주문)
            OrderRequest request = new OrderRequest(
                    List.of(
                            new OrderItemRequest(productId3, 6) // 재고 5개인데 6개 주문 (부족)
                    )
            );

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> orderFacade.createOrder(userId, request));
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("재고가 부족합니다");

            // assert - 트랜잭션 롤백 확인: 재고가 변경되지 않았는지 확인
            Supply supply3 = supplyJpaRepository.findByProductId(productId3).orElseThrow();
            assertThat(supply3.getStock().quantity()).isEqualTo(initialStock3);

            // assert - 포인트가 차감되지 않았는지 확인
            Point point = pointJpaRepository.findByUserId(userEntityId).orElseThrow();
            assertThat(point.getAmount()).isEqualTo(initialPoint);
        }

        @DisplayName("여러 상품 중 일부 재고 부족 시, 예외가 발생하고 트랜잭션이 롤백된다.")
        @Test
        void should_rollback_when_partialStockInsufficient() {
            // arrange
            // 초기 포인트: 100000원 (setup에서 설정)
            // 초기 재고: productId1 = 100개, productId3 = 5개
            // 주문 시도: 상품1 5개(50000원) + 상품3 6개(90000원) - 상품3 재고 부족
            // 재고 부족으로 예외 발생 → 롤백
            int initialStock1 = supplyJpaRepository.findByProductId(productId1).orElseThrow().getStock().quantity();
            int initialStock3 = supplyJpaRepository.findByProductId(productId3).orElseThrow().getStock().quantity();
            long initialPoint = pointJpaRepository.findByUserId(userEntityId).orElseThrow().getAmount();

            // productId1은 충분하지만 productId3는 부족
            OrderRequest request = new OrderRequest(
                    List.of(
                            new OrderItemRequest(productId1, 5), // 재고 충분 (100개)
                            new OrderItemRequest(productId3, 6) // 재고 부족 (5개만 있음)
                    )
            );

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> orderFacade.createOrder(userId, request));
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("재고가 부족합니다");

            // assert - 트랜잭션 롤백 확인: 모든 재고가 변경되지 않았는지 확인
            Supply supply1 = supplyJpaRepository.findByProductId(productId1).orElseThrow();
            assertThat(supply1.getStock().quantity()).isEqualTo(initialStock1);
            Supply supply3 = supplyJpaRepository.findByProductId(productId3).orElseThrow();
            assertThat(supply3.getStock().quantity()).isEqualTo(initialStock3);

            // assert - 포인트가 차감되지 않았는지 확인
            Point point = pointJpaRepository.findByUserId(userEntityId).orElseThrow();
            assertThat(point.getAmount()).isEqualTo(initialPoint);
        }

        @DisplayName("여러 상품 중 상품3의 재고가 부족할 때, 예외가 발생하고 트랜잭션이 롤백된다.")
        @Test
        void should_rollback_when_product3StockInsufficientInMultipleProducts() {
            // arrange
            // 초기 포인트: 100000원 (setup에서 설정)
            // 초기 재고: productId1 = 100개, productId2 = 50개, productId3 = 5개
            // 주문 시도: 상품1 2개(20000원) + 상품2 1개(20000원) + 상품3 6개(90000원) - 상품3 재고 부족
            // 재고 부족으로 예외 발생 → 롤백
            int initialStock1 = supplyJpaRepository.findByProductId(productId1).orElseThrow().getStock().quantity();
            int initialStock2 = supplyJpaRepository.findByProductId(productId2).orElseThrow().getStock().quantity();
            int initialStock3 = supplyJpaRepository.findByProductId(productId3).orElseThrow().getStock().quantity();
            long initialPoint = pointJpaRepository.findByUserId(userEntityId).orElseThrow().getAmount();

            // productId1, productId2는 충분하지만 productId3는 부족
            OrderRequest request = new OrderRequest(
                    List.of(
                            new OrderItemRequest(productId1, 2), // 재고 충분 (100개)
                            new OrderItemRequest(productId2, 1), // 재고 충분 (50개)
                            new OrderItemRequest(productId3, 6)  // 재고 부족 (5개만 있음)
                    )
            );

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> orderFacade.createOrder(userId, request));
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("재고가 부족합니다");

            // assert - 트랜잭션 롤백 확인: 모든 재고가 변경되지 않았는지 확인
            Supply supply1 = supplyJpaRepository.findByProductId(productId1).orElseThrow();
            assertThat(supply1.getStock().quantity()).isEqualTo(initialStock1);
            Supply supply2 = supplyJpaRepository.findByProductId(productId2).orElseThrow();
            assertThat(supply2.getStock().quantity()).isEqualTo(initialStock2);
            Supply supply3 = supplyJpaRepository.findByProductId(productId3).orElseThrow();
            assertThat(supply3.getStock().quantity()).isEqualTo(initialStock3);

            // assert - 포인트가 차감되지 않았는지 확인
            Point point = pointJpaRepository.findByUserId(userEntityId).orElseThrow();
            assertThat(point.getAmount()).isEqualTo(initialPoint);
        }

        @DisplayName("상품3의 재고가 정확히 0이 될 때, 주문이 성공한다. (Edge Case)")
        @Test
        void should_createOrder_when_product3StockExactlyZero() {
            // arrange
            // 초기 포인트: 100000원 (setup에서 설정)
            // 초기 재고: productId3 = 5개
            // 주문: 상품3 5개 = 75000원
            // 예상 남은 포인트: 100000 - 75000 = 25000원
            // 예상 남은 재고: 5 - 5 = 0개
            OrderRequest request = new OrderRequest(
                    List.of(
                            new OrderItemRequest(productId3, 5) // 재고 5개를 모두 주문
                    )
            );

            // act
            OrderInfo orderInfo = orderFacade.createOrder(userId, request);

            // assert - 주문 생성 확인
            assertThat(orderInfo).isNotNull();
            assertThat(orderInfo.totalPrice()).isEqualTo(75000); // 15000 * 5

            // assert - 재고가 정확히 0이 되었는지 확인
            Supply supply3 = supplyJpaRepository.findByProductId(productId3).orElseThrow();
            assertThat(supply3.getStock().quantity()).isEqualTo(0); // 5 - 5 = 0

            // assert - 포인트 차감 확인
            Point point = pointJpaRepository.findByUserId(userEntityId).orElseThrow();
            assertThat(point.getAmount()).isEqualTo(25000); // 100000 - 75000
        }

        @DisplayName("포인트 부족 시, 예외가 발생하고 트랜잭션이 롤백된다.")
        @Test
        void should_rollback_when_pointInsufficient() {
            // arrange
            // 초기 포인트: 100000원 (setup에서 설정)
            // 첫 번째 주문: 상품1 9개 = 90000원
            // 첫 번째 주문 후 포인트: 100000 - 90000 = 10000원
            OrderRequest firstOrder = new OrderRequest(
                    List.of(new OrderItemRequest(productId1, 9)) // 90000원 사용
            );
            orderFacade.createOrder(userId, firstOrder);
            // 남은 포인트: 10000원

            // 두 번째 주문 시도: 상품1 1개 = 10000원
            // 포인트가 정확히 일치하므로 성공해야 하지만, 테스트 목적상 포인트 부족 시나리오를 위해
            // 더 큰 금액의 주문을 시도
            int initialStock1 = supplyJpaRepository.findByProductId(productId1).orElseThrow().getStock().quantity();
            long initialPoint = pointJpaRepository.findByUserId(userEntityId).orElseThrow().getAmount();

            // 포인트가 부족한 주문 (20000원 필요하지만 10000원만 있음)
            OrderRequest request = new OrderRequest(
                    List.of(
                            new OrderItemRequest(productId2, 1) // 20000원 필요 (부족)
                    )
            );

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> orderFacade.createOrder(userId, request));
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("포인트가 부족합니다");

            // assert - 트랜잭션 롤백 확인: 재고가 변경되지 않았는지 확인
            Supply supply1 = supplyJpaRepository.findByProductId(productId1).orElseThrow();
            assertThat(supply1.getStock().quantity()).isEqualTo(initialStock1);

            // assert - 포인트가 차감되지 않았는지 확인
            Point pointAfter = pointJpaRepository.findByUserId(userEntityId).orElseThrow();
            assertThat(pointAfter.getAmount()).isEqualTo(initialPoint);
        }

        @DisplayName("동시에 같은 쿠폰을 사용하려고 할 때, Pessimistic Lock이 작동하여 하나만 성공한다.")
        @Test
        void concurrencyTest_couponShouldBeUsedOnlyOnceWhenConcurrentOrders() throws InterruptedException {
            // arrange
            // 초기 포인트: 100000원 (setup에서 설정)
            // 주문: 상품1 1개(10000원) + 정액 쿠폰 5000원 할인 = 5000원
            // 5건 동시 주문 시도 → 1건만 성공, 4건 실패
            // 성공한 1건: 포인트 5000원 차감 → 남은 포인트 95000원
            Coupon coupon = Coupon.issueFixed(userEntityId, 5000);
            Coupon savedCoupon = couponJpaRepository.save(coupon);
            Long couponId = savedCoupon.getId();

            OrderRequest request = new OrderRequest(
                    List.of(new OrderItemRequest(productId1, 1)), // 10000원, 쿠폰 할인 후 5000원
                    couponId
            );

            int threadCount = 5;
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);

            // act
            try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
                for (int i = 0; i < threadCount; i++) {
                    executor.submit(() -> {
                        TransactionTemplate template = new TransactionTemplate(transactionManager);
                        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);  // 새 트랜잭션 강제
                        template.execute(status -> {
                            try {
                                orderFacade.createOrder(userId, request);
                                successCount.incrementAndGet();
                            } catch (Exception e) {
                                System.out.println("실패: " + e.getMessage());
                                status.setRollbackOnly();  // 예외 시 명시 롤백
                                failureCount.incrementAndGet();
                            }
                            return null;
                        });
                        latch.countDown();
                    });
                }
            }

            latch.await();

            // assert - 정확히 하나의 주문만 성공했는지 확인
            assertThat(successCount.get()).isEqualTo(1);
            assertThat(failureCount.get()).isEqualTo(4);

            // assert - 쿠폰이 사용되었는지 확인
            Coupon usedCoupon = couponJpaRepository.findById(couponId).orElseThrow();
            assertThat(usedCoupon.isUsed()).isTrue();
        }

        @DisplayName("동시에 같은 상품의 재고를 차감할 때, Pessimistic Lock이 작동하여 정확히 차감된다.")
        @Test
        void concurrencyTest_stockShouldBeProperlyDecreasedWhenConcurrentOrders() throws InterruptedException {
            // arrange
            // 초기 포인트: 100000원 (setup에서 설정)
            // 초기 재고: productId1 = 100개
            // 주문: 상품1 1개씩 10건 동시 주문
            // 각 주문 금액: 10000원
            // 총 주문 금액: 10000원 * 10건 = 100000원
            // 예상 남은 포인트: 100000 - 100000 = 0원
            OrderRequest request = new OrderRequest(
                    List.of(new OrderItemRequest(productId1, 1)) // 10000원
            );

            int threadCount = 10;
            CountDownLatch latch = new CountDownLatch(threadCount);
            int initialStock = supplyJpaRepository.findByProductId(productId1).orElseThrow().getStock().quantity();

            // act
            try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
                for (int i = 0; i < threadCount; i++) {
                    executor.submit(() -> {
                        TransactionTemplate template = new TransactionTemplate(transactionManager);
                        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);  // 새 트랜잭션 강제
                        template.execute(status -> {
                            try {
                                orderFacade.createOrder(userId, request);
                            } catch (Exception e) {
                                System.out.println("실패: " + e.getMessage());
                                status.setRollbackOnly();  // 예외 시 명시 롤백
                            }
                            return null;
                        });
                        latch.countDown();
                    });
                }
            }

            latch.await();

            // assert - 재고가 정확히 차감되었는지 확인 (초기 재고 100에서 10개 차감 = 90)
            Supply supply = supplyJpaRepository.findByProductId(productId1).orElseThrow();
            assertThat(supply.getStock().quantity()).isEqualTo(initialStock - 10);

            // assert - 포인트가 정확히 차감되었는지 확인
            Point point = pointJpaRepository.findByUserId(userEntityId).orElseThrow();
            assertThat(point.getAmount()).isEqualTo(0); // 100000 - (10000 * 10) = 0
        }

        @DisplayName("동시에 같은 사용자의 포인트를 차감할 때, Pessimistic Lock이 작동하여 정확히 차감된다.")
        @Test
        void concurrencyTest_pointShouldBeProperlyDecreasedWhenConcurrentOrders() throws InterruptedException {
            // arrange
            // 초기 포인트: 100000원 (setup에서 설정)
            // 주문: 상품1 1개씩 5건 동시 주문
            // 각 주문 금액: 10000원
            // 총 주문 금액: 10000원 * 5건 = 50000원
            // 예상 남은 포인트: 100000 - 50000 = 50000원
            OrderRequest request = new OrderRequest(
                    List.of(new OrderItemRequest(productId1, 1)) // 10000원
            );

            int threadCount = 5;
            CountDownLatch latch = new CountDownLatch(threadCount);

            // act
            try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
                for (int i = 0; i < threadCount; i++) {
                    executor.submit(() -> {
                        TransactionTemplate template = new TransactionTemplate(transactionManager);
                        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);  // 새 트랜잭션 강제
                        template.execute(status -> {
                            try {
                                orderFacade.createOrder(userId, request);
                            } catch (Exception e) {
                                System.out.println("실패: " + e.getMessage());
                                status.setRollbackOnly();  // 예외 시 명시 롤백
                            }
                            return null;
                        });
                        latch.countDown();
                    });
                }
            }

            latch.await();

            // assert - 포인트가 정확히 차감되었는지 확인
            // 성공한 주문 수만큼 차감되어야 함 (초기 100000원에서 10000원씩 차감)
            Point point = pointJpaRepository.findByUserId(userEntityId).orElseThrow();
            // 5개 주문이 모두 성공했다면: 100000 - (10000 * 5) = 50000원
            assertThat(point.getAmount()).isEqualTo(50000); // 100000 - (10000 * 5)
        }
    }
}

