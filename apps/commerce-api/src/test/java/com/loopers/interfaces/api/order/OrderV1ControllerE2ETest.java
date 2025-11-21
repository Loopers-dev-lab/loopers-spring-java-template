package com.loopers.interfaces.api.order;

import com.loopers.domain.Money;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderV1ControllerE2ETest {

    private final TestRestTemplate testRestTemplate;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final DatabaseCleanUp databaseCleanUp;


    @Autowired
    public OrderV1ControllerE2ETest(
            TestRestTemplate testRestTemplate,
            UserRepository userRepository,
            ProductRepository productRepository,
            BrandRepository brandRepository,
            DatabaseCleanUp databaseCleanUp
    ) {
        this.testRestTemplate = testRestTemplate;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.brandRepository = brandRepository;
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
            Brand savedBrand = brandRepository.registerBrand(brand);

            Product product = Product.createProduct("P001", "테스트상품", Money.of(10000), 100, savedBrand);
            Product savedProduct = productRepository.registerProduct(product);

            User user = User.createUser("testuser", "test@test.com", "1990-01-01", Gender.MALE);
            user.chargePoint(Money.of(100000)); // 10만원 충전
            User savedUser = userRepository.save(user);

            // when
            List<OrderV1Dto.OrderRequest.OrderItemRequest> items = List.of(
                    new OrderV1Dto.OrderRequest.OrderItemRequest(savedProduct.getId(), 2)
            );

            OrderV1Dto.OrderRequest request = new OrderV1Dto.OrderRequest(
                    savedUser.getUserId(),
                    items
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
            assertThat(response.getBody().data().status()).isEqualTo(OrderStatus.INIT);
            assertThat(response.getBody().data().totalPrice()).isEqualByComparingTo(BigDecimal.valueOf(20000)); // 10,000 * 2
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
            Brand savedBrand = brandRepository.registerBrand(brand);

            Product product = Product.createProduct("P001", "테스트상품", Money.of(10000), 100, savedBrand);
            Product savedProduct = productRepository.registerProduct(product);

            // when
            List<OrderV1Dto.OrderRequest.OrderItemRequest> items = List.of(
                    new OrderV1Dto.OrderRequest.OrderItemRequest(savedProduct.getId(), 2)
            );

            OrderV1Dto.OrderRequest request = new OrderV1Dto.OrderRequest(
                    "nonExistentUser", // 존재하지 않는 사용자
                    items
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
            User savedUser = userRepository.save(user);

            // when
            List<OrderV1Dto.OrderRequest.OrderItemRequest> items = List.of(
                    new OrderV1Dto.OrderRequest.OrderItemRequest(99999L, 2) // 존재하지 않는 상품 ID
            );

            OrderV1Dto.OrderRequest request = new OrderV1Dto.OrderRequest(
                    savedUser.getUserId(),
                    items
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
            Brand savedBrand = brandRepository.registerBrand(brand);

            Product product = Product.createProduct("P001", "테스트상품", Money.of(10000), 5, savedBrand); // 재고 5개
            Product savedProduct = productRepository.registerProduct(product);

            User user = User.createUser("testuser", "test@test.com", "1990-01-01", Gender.MALE);
            user.chargePoint(Money.of(1000000)); // 충분한 포인트
            User savedUser = userRepository.save(user);

            // when
            List<OrderV1Dto.OrderRequest.OrderItemRequest> items = List.of(
                    new OrderV1Dto.OrderRequest.OrderItemRequest(savedProduct.getId(), 10) // 재고보다 많은 수량
            );

            OrderV1Dto.OrderRequest request = new OrderV1Dto.OrderRequest(
                    savedUser.getUserId(),
                    items
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
            Brand savedBrand = brandRepository.registerBrand(brand);

            Product product = Product.createProduct("P001", "테스트상품", Money.of(10000), 100, savedBrand);
            Product savedProduct = productRepository.registerProduct(product);

            User user = User.createUser("testuser", "test@test.com", "1990-01-01", Gender.MALE);
            user.chargePoint(Money.of(5000)); // 부족한 포인트
            User savedUser = userRepository.save(user);

            // when
            List<OrderV1Dto.OrderRequest.OrderItemRequest> items = List.of(
                    new OrderV1Dto.OrderRequest.OrderItemRequest(savedProduct.getId(), 2) // 총 20,000원
            );

            OrderV1Dto.OrderRequest request = new OrderV1Dto.OrderRequest(
                    savedUser.getUserId(),
                    items
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
            Brand savedBrand = brandRepository.registerBrand(brand);

            Product product = Product.createProduct("P001", "테스트상품", Money.of(10000), 100, savedBrand);
            Product savedProduct = productRepository.registerProduct(product);

            User user = User.createUser("testuser", "test@test.com", "1990-01-01", Gender.MALE);
            user.chargePoint(Money.of(100000));
            User savedUser = userRepository.save(user);

            // when
            List<OrderV1Dto.OrderRequest.OrderItemRequest> items = List.of(
                    new OrderV1Dto.OrderRequest.OrderItemRequest(savedProduct.getId(), 0) // 수량 0
            );

            OrderV1Dto.OrderRequest request = new OrderV1Dto.OrderRequest(
                    savedUser.getUserId(),
                    items
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
            Brand savedBrand = brandRepository.registerBrand(brand);

            Product product = Product.createProduct("P001", "테스트상품", Money.of(10000), 100, savedBrand);
            Product savedProduct = productRepository.registerProduct(product);

            User user = User.createUser("testuser", "test@test.com", "1990-01-01", Gender.MALE);
            user.chargePoint(Money.of(100000));
            User savedUser = userRepository.save(user);

            // when
            List<OrderV1Dto.OrderRequest.OrderItemRequest> items = List.of(
                    new OrderV1Dto.OrderRequest.OrderItemRequest(savedProduct.getId(), -1) // 음수 수량
            );

            OrderV1Dto.OrderRequest request = new OrderV1Dto.OrderRequest(
                    savedUser.getUserId(),
                    items
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
            User savedUser = userRepository.save(user);

            // when
            OrderV1Dto.OrderRequest request = new OrderV1Dto.OrderRequest(
                    savedUser.getUserId(),
                    List.of() // 빈 항목 리스트
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
