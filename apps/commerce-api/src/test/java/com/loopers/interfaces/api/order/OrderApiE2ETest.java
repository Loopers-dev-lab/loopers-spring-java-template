package com.loopers.interfaces.api.order;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.point.PointAccount;
import com.loopers.domain.product.Product;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.point.PointAccountJpaRepository;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderApiE2ETest {

    private static final String ENDPOINT = "/api/v1/orders";

    private final TestRestTemplate testRestTemplate;
    private final UserJpaRepository userJpaRepository;
    private final BrandJpaRepository brandJpaRepository;
    private final ProductJpaRepository productJpaRepository;
    private final PointAccountJpaRepository pointAccountJpaRepository;
    private final DatabaseCleanUp databaseCleanUp;

    @Autowired
    public OrderApiE2ETest(
            TestRestTemplate testRestTemplate,
            UserJpaRepository userJpaRepository,
            BrandJpaRepository brandJpaRepository,
            ProductJpaRepository productJpaRepository,
            PointAccountJpaRepository pointAccountJpaRepository,
            DatabaseCleanUp databaseCleanUp
    ) {
        this.testRestTemplate = testRestTemplate;
        this.userJpaRepository = userJpaRepository;
        this.brandJpaRepository = brandJpaRepository;
        this.productJpaRepository = productJpaRepository;
        this.pointAccountJpaRepository = pointAccountJpaRepository;
        this.databaseCleanUp = databaseCleanUp;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("POST /api/v1/orders")
    @Nested
    class CreateOrder {

        @DisplayName("주문 성공")
        @Test
        void orderTest1() {
            // arrange
            User user = userJpaRepository.save(
                    User.create("user123", "user@test.com", "2000-01-01", Gender.MALE)
            );

            PointAccount pointAccount = pointAccountJpaRepository.save(
                    PointAccount.create(user.getUserId())
            );
            pointAccount.charge(100_000L);
            pointAccountJpaRepository.save(pointAccount);

            Brand brand = brandJpaRepository.save(Brand.create("브랜드A"));

            Product product1 = productJpaRepository.save(
                    Product.create("상품1", "설명1", 10_000, 100L, brand.getId())
            );

            Product product2 = productJpaRepository.save(
                    Product.create("상품2", "설명2", 20_000, 50L, brand.getId())
            );

            OrderDto.OrderCreateRequest request = new OrderDto.OrderCreateRequest(
                    List.of(
                            new OrderDto.OrderItemRequest(product1.getId(), 2L),
                            new OrderDto.OrderItemRequest(product2.getId(), 1L)
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", user.getUserId());
            HttpEntity<OrderDto.OrderCreateRequest> httpEntity = new HttpEntity<>(request, headers);

            // act
            ParameterizedTypeReference<ApiResponse<OrderDto.OrderResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };

            ResponseEntity<ApiResponse<OrderDto.OrderResponse>> response =
                    testRestTemplate.exchange(ENDPOINT, HttpMethod.POST, httpEntity, responseType);

            // assert
            assertAll(
                    () -> assertThat(response.getStatusCode().is2xxSuccessful()).isTrue(),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data().userId()).isEqualTo(user.getUserId()),
                    () -> assertThat(response.getBody().data().items()).hasSize(2),
                    () -> assertThat(response.getBody().data().totalAmount()).isEqualTo(40_000),
                    () -> {
                        Product updatedProduct1 = productJpaRepository.findById(product1.getId()).get();
                        assertThat(updatedProduct1.getStock()).isEqualTo(98L);
                    },
                    () -> {
                        Product updatedProduct2 = productJpaRepository.findById(product2.getId()).get();
                        assertThat(updatedProduct2.getStock()).isEqualTo(49L);
                    },
                    () -> {
                        PointAccount updatedAccount = pointAccountJpaRepository.findByUserId(user.getUserId()).get();
                        assertThat(updatedAccount.getBalance().amount()).isEqualTo(60_000L);
                    }
            );
        }
    }
}
