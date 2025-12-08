package com.loopers.interfaces.api.order;

import com.loopers.domain.Money;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.DiscountType;
import com.loopers.domain.issuedcoupon.IssuedCoupon;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.payment.PaymentType;
import com.loopers.domain.product.Product;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.coupon.CouponJpaRepository;
import com.loopers.infrastructure.issuedcoupon.IssuedCouponJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderV1ControllerE2ETest {

    private final TestRestTemplate testRestTemplate;
    private final UserJpaRepository userJpaRepository;
    private final ProductJpaRepository productJpaRepository;
    private final BrandJpaRepository brandJpaRepository;
    private final CouponJpaRepository couponJpaRepository;
    private final IssuedCouponJpaRepository issuedCouponJpaRepository;
    private final DatabaseCleanUp databaseCleanUp;


    @Autowired
    public OrderV1ControllerE2ETest(
            TestRestTemplate testRestTemplate,
            UserJpaRepository userJpaRepository,
            ProductJpaRepository productJpaRepository,
            BrandJpaRepository brandJpaRepository,
            CouponJpaRepository couponJpaRepository,
            IssuedCouponJpaRepository issuedCouponJpaRepository,
            DatabaseCleanUp databaseCleanUp
    ) {
        this.testRestTemplate = testRestTemplate;
        this.userJpaRepository = userJpaRepository;
        this.productJpaRepository = productJpaRepository;
        this.brandJpaRepository = brandJpaRepository;
        this.couponJpaRepository = couponJpaRepository;
        this.issuedCouponJpaRepository = issuedCouponJpaRepository;
        this.databaseCleanUp = databaseCleanUp;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("POST /api/v1/orders/new")
    @Nested
    class CreateOrder {

        @DisplayName("주문 생성에 성공할 경우 주문 정보를 반환한다.")
        @Test
        void createOrderSuccess_returnOrderInfo() {
            // given
            Brand brand = Brand.createBrand("테스트브랜드");
            Brand savedBrand = brandJpaRepository.save(brand);

            Product product = Product.createProduct("P001", "테스트상품", Money.of(10000), 100, savedBrand);
            Product savedProduct = productJpaRepository.save(product);

            User user = User.createUser("testuser", "test@test.com", "1990-01-01", Gender.MALE);
            user.chargePoint(Money.of(100000)); // 10만원 충전
            User savedUser = userJpaRepository.save(user);

            LocalDate today = LocalDate.now();

            Coupon coupon = Coupon.createCoupon("COUPON123456", "테스트쿠폰", "테스트 쿠폰입니다",
                    today.minusDays(1), today.plusDays(30), DiscountType.RATE, 10);
            Coupon savedCoupon = couponJpaRepository.save(coupon);

            IssuedCoupon issuedCoupon = IssuedCoupon.issue(savedUser, savedCoupon);
            issuedCouponJpaRepository.save(issuedCoupon);

            // when
            List<OrderV1Dto.OrderRequest.OrderItemRequest> items = List.of(
                    new OrderV1Dto.OrderRequest.OrderItemRequest(savedProduct.getId(), 2)
            );

            OrderV1Dto.OrderRequest request = new OrderV1Dto.OrderRequest(
                    savedUser.getUserId(),
                    items,
                    savedCoupon.getId(),
                    PaymentType.POINT,
                    null,
                    null
            );

            ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> response = testRestTemplate.exchange(
                    "/api/v1/orders/new",
                    HttpMethod.POST,
                    new HttpEntity<>(request),
                    new ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
            );

            // then - 검증
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().data()).isNotNull();
            assertThat(response.getBody().data().id()).isNotNull();
            assertThat(response.getBody().data().status()).isEqualTo(OrderStatus.COMPLETED);
            assertThat(response.getBody().data().totalPrice()).isEqualByComparingTo(BigDecimal.valueOf(18000)); // 10,000 * 2 * 0.9 (10% 할인)
        }

        @DisplayName("동일한 유저가 동시에 주문을 요청해도 포인트가 정상적으로 차감된다.")
        @Test
        void createOrder_withConcurrentRequests_deductsPointsCorrectly() {
            // given
            Brand brand = Brand.createBrand("테스트브랜드");
            Brand savedBrand = brandJpaRepository.save(brand);

            Product product = Product.createProduct("P001", "테스트상품", Money.of(10000), 100, savedBrand);
            Product savedProduct = productJpaRepository.save(product);

            User user = User.createUser("testuser", "test@test.com", "1990-01-01", Gender.MALE);
            user.chargePoint(Money.of(100000)); // 10만원 충전
            User savedUser = userJpaRepository.save(user);

            int numberOfThreads = 2;
            ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

            List<OrderV1Dto.OrderRequest.OrderItemRequest> items = List.of(
                    new OrderV1Dto.OrderRequest.OrderItemRequest(savedProduct.getId(), 2) // 20,000원
            );
            OrderV1Dto.OrderRequest request = new OrderV1Dto.OrderRequest(
                    savedUser.getUserId(), items, null, PaymentType.POINT, null, null
            );

            // when
            List<CompletableFuture<Void>> futures = new java.util.ArrayList<>();
            for (int i = 0; i < numberOfThreads; i++) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    testRestTemplate.exchange(
                            "/api/v1/orders/new",
                            HttpMethod.POST,
                            new HttpEntity<>(request),
                            new ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
                    );
                }, executorService);
                futures.add(future);
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join(); // 모든 비동기 작업이 완료될 때까지 대기
            executorService.shutdown();

            // then
            User finalUser = userJpaRepository.findByUserId(savedUser.getUserId());
            assertThat(finalUser.getPoint().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(60000)); // 100,000 - 20,000 * 2 = 60,000

            Product finalProduct = productJpaRepository.findById(savedProduct.getId()).orElseThrow();
            assertThat(finalProduct.getStock().getQuantity()).isEqualTo(96); // 100 - 2 * 2
        }

        @DisplayName("동일한 쿠폰으로 여러 기기에서 동시에 주문해도, 쿠폰은 단 한번만 사용된다.")
        @Test
        void createOrder_withSameCouponConcurrently_usesOnlyOnce() {
            // given
            Brand brand = Brand.createBrand("테스트브랜드");
            Brand savedBrand = brandJpaRepository.save(brand);

            Product product = Product.createProduct("P001", "테스트상품", Money.of(10000), 100, savedBrand);
            Product savedProduct = productJpaRepository.save(product);

            User user = User.createUser("testuser", "test@test.com", "1990-01-01", Gender.MALE);
            user.chargePoint(Money.of(100000)); // 10만원 충전
            User savedUser = userJpaRepository.save(user);

            LocalDate today = LocalDate.now();

            Coupon coupon = Coupon.createCoupon("COUPON123456", "테스트쿠폰", "테스트 쿠폰입니다",
                    today.minusDays(1), today.plusDays(30), DiscountType.RATE, 10);
            Coupon savedCoupon = couponJpaRepository.save(coupon);

            IssuedCoupon issuedCoupon = IssuedCoupon.issue(savedUser, savedCoupon);
            IssuedCoupon savedIssuedCoupon = issuedCouponJpaRepository.save(issuedCoupon);

            int numberOfThreads = 2;
            ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

            List<OrderV1Dto.OrderRequest.OrderItemRequest> items = List.of(
                    new OrderV1Dto.OrderRequest.OrderItemRequest(savedProduct.getId(), 2) // 20,000원
            );
            OrderV1Dto.OrderRequest request = new OrderV1Dto.OrderRequest(
                    savedUser.getUserId(),
                    items,
                    savedCoupon.getId(),
                    PaymentType.POINT,
                    null,
                    null
            );

            // when - 동일한 쿠폰으로 동시에 2번 주문 시도
            List<CompletableFuture<ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>>>> futures = new java.util.ArrayList<>();
            for (int i = 0; i < numberOfThreads; i++) {
                CompletableFuture<ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>>> future = CompletableFuture.supplyAsync(() -> {
                    return testRestTemplate.exchange(
                            "/api/v1/orders/new",
                            HttpMethod.POST,
                            new HttpEntity<>(request),
                            new ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
                    );
                }, executorService);
                futures.add(future);
            }

            List<ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>>> responses = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();
            executorService.shutdown();

            // then - 동일한 쿠폰으로 여러 기기에서 동시에 주문해도, 쿠폰은 단 한번만 사용되어야 함
            long successCount = responses.stream().filter(r -> r.getStatusCode() == HttpStatus.OK).count();
            long failCount = responses.stream().filter(r -> r.getStatusCode() != HttpStatus.OK).count();

            assertThat(successCount).isEqualTo(1); // 1개만 성공
            assertThat(failCount).isEqualTo(1); // 1개는 실패 (쿠폰 이미 사용됨)

            User finalUser = userJpaRepository.findByUserId(savedUser.getUserId());
            assertThat(finalUser.getPoint().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(82000)); // 100,000 - (20,000 * 0.9) = 82,000

            Product finalProduct = productJpaRepository.findById(savedProduct.getId()).orElseThrow();
            assertThat(finalProduct.getStock().getQuantity()).isEqualTo(98); // 100 - 2 (한 번만 주문 성공)
        }
    }

    @DisplayName("POST /api/v1/orders/new - 예외 케이스")
    @Nested
    class CreateOrderExceptionCases {

        @DisplayName("존재하지 않는 사용자로 주문 시도 시 실패한다.")
        @Test
        void createOrder_withNonExistentUser_fail() {
            // given
            Brand brand = Brand.createBrand("테스트브랜드");
            Brand savedBrand = brandJpaRepository.save(brand);

            Product product = Product.createProduct("P001", "테스트상품", Money.of(10000), 100, savedBrand);
            Product savedProduct = productJpaRepository.save(product);

            // when
            List<OrderV1Dto.OrderRequest.OrderItemRequest> items = List.of(
                    new OrderV1Dto.OrderRequest.OrderItemRequest(savedProduct.getId(), 2)
            );

            OrderV1Dto.OrderRequest request = new OrderV1Dto.OrderRequest(
                    "nonExistentUser", // 존재하지 않는 사용자
                    items,
                    null,
                    PaymentType.POINT,
                    null,
                    null
            );

            ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> response = testRestTemplate.exchange(
                    "/api/v1/orders/new",
                    HttpMethod.POST,
                    new HttpEntity<>(request),
                    new ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().meta().result()).isEqualTo(ApiResponse.Metadata.Result.FAIL);
        }

        @DisplayName("존재하지 않는 상품으로 주문 시도 시 실패한다.")
        @Test
        void createOrder_withNonExistentProduct_fail() {
            // given
            User user = User.createUser("testuser", "test@test.com", "1990-01-01", Gender.MALE);
            user.chargePoint(Money.of(100000));
            User savedUser = userJpaRepository.save(user);

            // when
            List<OrderV1Dto.OrderRequest.OrderItemRequest> items = List.of(
                    new OrderV1Dto.OrderRequest.OrderItemRequest(99999L, 2) // 존재하지 않는 상품 ID
            );

            OrderV1Dto.OrderRequest request = new OrderV1Dto.OrderRequest(
                    savedUser.getUserId(),
                    items,
                    null,
                    PaymentType.POINT,
                    null,
                    null
            );

            ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> response = testRestTemplate.exchange(
                    "/api/v1/orders/new",
                    HttpMethod.POST,
                    new HttpEntity<>(request),
                    new ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().meta().result()).isEqualTo(ApiResponse.Metadata.Result.FAIL);
        }

        @DisplayName("재고가 부족한 경우 주문에 실패한다.")
        @Test
        void createOrder_withInsufficientStock_fail() {
            // given
            Brand brand = Brand.createBrand("테스트브랜드");
            Brand savedBrand = brandJpaRepository.save(brand);

            Product product = Product.createProduct("P001", "테스트상품", Money.of(10000), 5, savedBrand); // 재고 5개
            Product savedProduct = productJpaRepository.save(product);

            User user = User.createUser("testuser", "test@test.com", "1990-01-01", Gender.MALE);
            user.chargePoint(Money.of(1000000)); // 충분한 포인트
            User savedUser = userJpaRepository.save(user);

            LocalDate today = LocalDate.now();

            Coupon coupon = Coupon.createCoupon("COUPON123456", "테스트쿠폰", "테스트 쿠폰입니다",
                    today.minusDays(1), today.plusDays(30), DiscountType.RATE, 10);
            Coupon savedCoupon = couponJpaRepository.save(coupon);

            IssuedCoupon issuedCoupon = IssuedCoupon.issue(savedUser, savedCoupon);
            issuedCouponJpaRepository.save(issuedCoupon);

            // when
            List<OrderV1Dto.OrderRequest.OrderItemRequest> items = List.of(
                    new OrderV1Dto.OrderRequest.OrderItemRequest(savedProduct.getId(), 10) // 재고보다 많은 수량
            );

            OrderV1Dto.OrderRequest request = new OrderV1Dto.OrderRequest(
                    savedUser.getUserId(),
                    items,
                    savedCoupon.getId(),
                    PaymentType.POINT,
                    null,
                    null
            );

            ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> response = testRestTemplate.exchange(
                    "/api/v1/orders/new",
                    HttpMethod.POST,
                    new HttpEntity<>(request),
                    new ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().meta().result()).isEqualTo(ApiResponse.Metadata.Result.FAIL);
        }

        @DisplayName("사용자 포인트가 부족한 경우 주문에 실패한다.")
        @Test
        void createOrder_withInsufficientPoint_fail() {
            // given
            Brand brand = Brand.createBrand("테스트브랜드");
            Brand savedBrand = brandJpaRepository.save(brand);

            Product product = Product.createProduct("P001", "테스트상품", Money.of(10000), 100, savedBrand);
            Product savedProduct = productJpaRepository.save(product);

            User user = User.createUser("testuser", "test@test.com", "1990-01-01", Gender.MALE);
            user.chargePoint(Money.of(5000)); // 부족한 포인트
            User savedUser = userJpaRepository.save(user);

            LocalDate today = LocalDate.now();

            Coupon coupon = Coupon.createCoupon("COUPON123456", "테스트쿠폰", "테스트 쿠폰입니다",
                    today.minusDays(1), today.plusDays(30), DiscountType.RATE, 10);
            Coupon savedCoupon = couponJpaRepository.save(coupon);

            IssuedCoupon issuedCoupon = IssuedCoupon.issue(savedUser, savedCoupon);
            issuedCouponJpaRepository.save(issuedCoupon);

            // when
            List<OrderV1Dto.OrderRequest.OrderItemRequest> items = List.of(
                    new OrderV1Dto.OrderRequest.OrderItemRequest(savedProduct.getId(), 2) // 총 20,000원
            );

            OrderV1Dto.OrderRequest request = new OrderV1Dto.OrderRequest(
                    savedUser.getUserId(),
                    items,
                    savedCoupon.getId(),
                    PaymentType.POINT,
                    null,
                    null
            );

            ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> response = testRestTemplate.exchange(
                    "/api/v1/orders/new",
                    HttpMethod.POST,
                    new HttpEntity<>(request),
                    new ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().meta().result()).isEqualTo(ApiResponse.Metadata.Result.FAIL);
        }

        @DisplayName("주문 수량이 0인 경우 주문에 실패한다.")
        @Test
        void createOrder_withZeroQuantity_fail() {
            // given
            Brand brand = Brand.createBrand("테스트브랜드");
            Brand savedBrand = brandJpaRepository.save(brand);

            Product product = Product.createProduct("P001", "테스트상품", Money.of(10000), 100, savedBrand);
            Product savedProduct = productJpaRepository.save(product);

            User user = User.createUser("testuser", "test@test.com", "1990-01-01", Gender.MALE);
            user.chargePoint(Money.of(100000));
            User savedUser = userJpaRepository.save(user);

            // when
            List<OrderV1Dto.OrderRequest.OrderItemRequest> items = List.of(
                    new OrderV1Dto.OrderRequest.OrderItemRequest(savedProduct.getId(), 0) // 수량 0
            );

            OrderV1Dto.OrderRequest request = new OrderV1Dto.OrderRequest(
                    savedUser.getUserId(),
                    items,
                    null,
                    PaymentType.POINT,
                    null,
                    null
            );

            ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> response = testRestTemplate.exchange(
                    "/api/v1/orders/new",
                    HttpMethod.POST,
                    new HttpEntity<>(request),
                    new ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().meta().result()).isEqualTo(ApiResponse.Metadata.Result.FAIL);
        }

        @DisplayName("주문 수량이 음수인 경우 주문에 실패한다.")
        @Test
        void createOrder_withNegativeQuantity_fail() {
            // given
            Brand brand = Brand.createBrand("테스트브랜드");
            Brand savedBrand = brandJpaRepository.save(brand);

            Product product = Product.createProduct("P001", "테스트상품", Money.of(10000), 100, savedBrand);
            Product savedProduct = productJpaRepository.save(product);

            User user = User.createUser("testuser", "test@test.com", "1990-01-01", Gender.MALE);
            user.chargePoint(Money.of(100000));
            User savedUser = userJpaRepository.save(user);

            // when
            List<OrderV1Dto.OrderRequest.OrderItemRequest> items = List.of(
                    new OrderV1Dto.OrderRequest.OrderItemRequest(savedProduct.getId(), -1) // 음수 수량
            );

            OrderV1Dto.OrderRequest request = new OrderV1Dto.OrderRequest(
                    savedUser.getUserId(),
                    items,
                    null,
                    PaymentType.POINT,
                    null,
                    null
            );

            ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> response = testRestTemplate.exchange(
                    "/api/v1/orders/new",
                    HttpMethod.POST,
                    new HttpEntity<>(request),
                    new ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().meta().result()).isEqualTo(ApiResponse.Metadata.Result.FAIL);
        }

        @DisplayName("주문 항목이 비어있는 경우 주문에 실패한다.")
        @Test
        void createOrder_withEmptyItems_fail() {
            // given
            User user = User.createUser("testuser", "test@test.com", "1990-01-01", Gender.MALE);
            user.chargePoint(Money.of(100000));
            User savedUser = userJpaRepository.save(user);

            // when
            OrderV1Dto.OrderRequest request = new OrderV1Dto.OrderRequest(
                    savedUser.getUserId(),
                    List.of(), // 빈 항목 리스트
                    null,
                    PaymentType.POINT,
                    null,
                    null
            );

            ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> response = testRestTemplate.exchange(
                    "/api/v1/orders/new",
                    HttpMethod.POST,
                    new HttpEntity<>(request),
                    new ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().meta().result()).isEqualTo(ApiResponse.Metadata.Result.FAIL);
        }
    }

}
