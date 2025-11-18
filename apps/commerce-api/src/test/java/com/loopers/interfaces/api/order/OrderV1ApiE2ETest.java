package com.loopers.interfaces.api.order;

import com.loopers.application.members.MemberFacade;
import com.loopers.domain.members.enums.Gender;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.repository.BrandRepository;
import com.loopers.domain.common.vo.Money;
import com.loopers.domain.points.Point;
import com.loopers.domain.points.repository.PointRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.repository.ProductRepository;
import com.loopers.domain.product.vo.Stock;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderV1ApiE2ETest {

    private final TestRestTemplate testRestTemplate;
    private final MemberFacade memberFacade;
    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final PointRepository pointRepository;
    private final DatabaseCleanUp databaseCleanUp;

    @Autowired
    public OrderV1ApiE2ETest(
            TestRestTemplate testRestTemplate,
            MemberFacade memberFacade,
            BrandRepository brandRepository,
            ProductRepository productRepository,
            PointRepository pointRepository,
            DatabaseCleanUp databaseCleanUp
    ) {
        this.testRestTemplate = testRestTemplate;
        this.memberFacade = memberFacade;
        this.brandRepository = brandRepository;
        this.productRepository = productRepository;
        this.pointRepository = pointRepository;
        this.databaseCleanUp = databaseCleanUp;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    private Long setupProductAndMemberWithPoints() {
        // 회원 생성
        memberFacade.registerMember("test123", "test@example.com", "password", "1990-01-01", Gender.MALE);

        // 충분한 포인트 지급 (기존 포인트를 조회하여 추가)
        Point memberPoint = pointRepository.findByMemberId("test123")
                .orElseThrow(() -> new IllegalStateException("회원의 포인트가 존재하지 않습니다"));
        memberPoint.addAmount(BigDecimal.valueOf(50000));
        pointRepository.save(memberPoint);

        // 브랜드 생성
        Brand brand = brandRepository.save(new Brand("TestBrand", "Test Brand Description"));

        // 상품 생성
        Product product = new Product(
                brand.getId(),
                "Test Product",
                "Test Description",
                Money.of(BigDecimal.valueOf(10000)),
                Stock.of(100)
        );
        return productRepository.save(product).getId();
    }

    @DisplayName("주문 생성 (POST /api/v1/orders)")
    @Nested
    class PlaceOrder {

        @DisplayName("유효한 주문 요청으로 주문 생성 시 200과 주문 정보를 반환한다")
        @Test
        void shouldReturn200AndOrderInfo_whenValidOrderRequest() {
            Long productId = setupProductAndMemberWithPoints();

            OrderV1Dto.PlaceOrderRequest request = new OrderV1Dto.PlaceOrderRequest(
                    List.of(new OrderV1Dto.OrderLineRequest(productId, 2))
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", "test123");

            ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> response =
                    testRestTemplate.exchange("/api/v1/orders", HttpMethod.POST,
                            new HttpEntity<>(request, headers), responseType);

            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data()).isNotNull(),
                    () -> assertThat(response.getBody().data().id()).isNotNull(),
                    () -> assertThat(response.getBody().data().totalPrice()).isEqualByComparingTo(BigDecimal.valueOf(20000)),
                    () -> assertThat(response.getBody().data().items()).hasSize(1)
            );
        }

        @DisplayName("재고 부족 시 400 Bad Request를 반환한다")
        @Test
        void shouldReturn400_whenInsufficientStock() {
            Long productId = setupProductAndMemberWithPoints();

            OrderV1Dto.PlaceOrderRequest request = new OrderV1Dto.PlaceOrderRequest(
                    List.of(new OrderV1Dto.OrderLineRequest(productId, 200)) // 재고보다 많은 수량
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", "test123");

            ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> response =
                    testRestTemplate.exchange("/api/v1/orders", HttpMethod.POST,
                            new HttpEntity<>(request, headers), responseType);

            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
            );
        }

        @DisplayName("포인트 부족 시 400 Bad Request를 반환한다")
        @Test
        void shouldReturn400_whenInsufficientPoints() {
            // 포인트 없는 회원과 상품 설정
            memberFacade.registerMember("pooruser", "poor@example.com", "password", "1990-01-01", Gender.MALE);
            
            Brand brand = brandRepository.save(new Brand("TestBrand", "Test Brand Description"));
            Product product = new Product(
                    brand.getId(),
                    "Expensive Product",
                    "Very expensive",
                    Money.of(BigDecimal.valueOf(100000)), // 비싼 상품
                    Stock.of(100)
            );
            Long productId = productRepository.save(product).getId();

            OrderV1Dto.PlaceOrderRequest request = new OrderV1Dto.PlaceOrderRequest(
                    List.of(new OrderV1Dto.OrderLineRequest(productId, 1))
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", "pooruser");

            ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> response =
                    testRestTemplate.exchange("/api/v1/orders", HttpMethod.POST,
                            new HttpEntity<>(request, headers), responseType);

            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
            );
        }
    }
}
