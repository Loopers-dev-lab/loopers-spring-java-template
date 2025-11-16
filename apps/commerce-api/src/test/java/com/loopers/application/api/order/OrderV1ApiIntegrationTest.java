package com.loopers.application.api.order;

import com.loopers.application.api.ApiIntegrationTest;
import com.loopers.application.api.common.dto.ApiResponse;
import com.loopers.core.domain.brand.Brand;
import com.loopers.core.domain.brand.repository.BrandRepository;
import com.loopers.core.domain.brand.vo.BrandDescription;
import com.loopers.core.domain.brand.vo.BrandName;
import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.repository.ProductRepository;
import com.loopers.core.domain.product.vo.ProductName;
import com.loopers.core.domain.product.vo.ProductPrice;
import com.loopers.core.domain.product.vo.ProductStock;
import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.UserPoint;
import com.loopers.core.domain.user.repository.UserPointRepository;
import com.loopers.core.domain.user.repository.UserRepository;
import com.loopers.core.domain.user.type.UserGender;
import com.loopers.core.domain.user.vo.*;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.List;

import static com.loopers.application.api.order.OrderV1Dto.OrderRequest;
import static com.loopers.application.api.order.OrderV1Dto.OrderResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;

class OrderV1ApiIntegrationTest extends ApiIntegrationTest {

    @Autowired
    UserRepository userRepository;

    @Autowired
    BrandRepository brandRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    UserPointRepository userPointRepository;

    String userIdentifier;
    String productId;
    Product product;

    @Nested
    @DisplayName("주문 요청")
    class 주문_요청 {

        @Nested
        @DisplayName("정상 요청인 경우")
        class 정상_요청인_경우 {

            @BeforeEach
            void setUp() {
                User user = userRepository.save(
                        User.create(
                                UserIdentifier.create("kilian"),
                                UserEmail.create("kilian@gmail.com"),
                                UserBirthDay.create("1997-10-08"),
                                UserGender.create("MALE")
                        )
                );
                userIdentifier = user.getIdentifier().value();
                userPointRepository.save(
                        Instancio.of(UserPoint.class)
                                .set(field("id"), UserPointId.empty())
                                .set(field("userId"), user.getUserId())
                                .set(field("balance"), new UserPointBalance(new BigDecimal(100_000)))
                                .create()
                );

                Brand brand = brandRepository.save(
                        Brand.create(new BrandName("kilian"), new BrandDescription("향수 브랜드"))
                );

                product = productRepository.save(
                        Product.create(
                                brand.getId(),
                                new ProductName("엔젤스 쉐어"),
                                new ProductPrice(new BigDecimal("150.00")),
                                new ProductStock(10L)
                        )
                );
                productId = product.getId().value();
            }

            @Test
            @DisplayName("주문을 생성하고 주문 ID를 반환한다.")
            void 주문을_생성하고_주문_ID를_반환한다() {
                // When
                String endPoint = "/api/v1/orders";
                HttpHeaders headers = new HttpHeaders();
                headers.set("X-USER-ID", userIdentifier);
                headers.setContentType(MediaType.APPLICATION_JSON);

                OrderRequest request = new OrderRequest(
                        List.of(
                                new OrderRequest.OrderItemRequest(productId, 2L)
                        )
                );

                HttpEntity<OrderRequest> httpEntity = new HttpEntity<>(request, headers);

                ParameterizedTypeReference<ApiResponse<OrderResponse>> responseType =
                        new ParameterizedTypeReference<>() {
                        };

                ResponseEntity<ApiResponse<OrderResponse>> response =
                        testRestTemplate.exchange(endPoint, HttpMethod.POST, httpEntity, responseType);

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().data()).isNotNull();
                assertThat(response.getBody().data().orderId()).isNotBlank();
            }
        }

        @Nested
        @DisplayName("X-USER-ID 헤더가 없을 경우")
        class X_USER_ID_헤더가_없을_경우 {

            @BeforeEach
            void setUp() {
                Brand brand = brandRepository.save(
                        Brand.create(new BrandName("kilian"), new BrandDescription("향수 브랜드"))
                );

                product = productRepository.save(
                        Product.create(
                                brand.getId(),
                                new ProductName("엔젤스 쉐어"),
                                new ProductPrice(new BigDecimal("150.00")),
                                new ProductStock(10L)
                        )
                );
                productId = product.getId().value();
            }

            @Test
            @DisplayName("400 Bad Request 응답을 반환한다.")
            void badRequest응답을_반환한다() {
                // When
                String endPoint = "/api/v1/orders";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                OrderRequest request = new OrderRequest(
                        List.of(
                                new OrderRequest.OrderItemRequest(productId, 2L)
                        )
                );

                HttpEntity<OrderRequest> httpEntity = new HttpEntity<>(request, headers);

                ParameterizedTypeReference<ApiResponse<Void>> responseType =
                        new ParameterizedTypeReference<>() {
                        };

                ResponseEntity<ApiResponse<Void>> response =
                        testRestTemplate.exchange(endPoint, HttpMethod.POST, httpEntity, responseType);

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            }
        }

        @Nested
        @DisplayName("존재하지 않는 사용자로 요청할 경우")
        class 존재하지_않는_사용자로_요청 {

            @BeforeEach
            void setUp() {
                Brand brand = brandRepository.save(
                        Brand.create(new BrandName("kilian"), new BrandDescription("향수 브랜드"))
                );

                product = productRepository.save(
                        Product.create(
                                brand.getId(),
                                new ProductName("엔젤스 쉐어"),
                                new ProductPrice(new BigDecimal("150.00")),
                                new ProductStock(10L)
                        )
                );
                productId = product.getId().value();
            }

            @Test
            @DisplayName("404 Not Found 응답을 반환한다.")
            void notFound응답을_반환한다() {
                // When
                String endPoint = "/api/v1/orders";
                HttpHeaders headers = new HttpHeaders();
                headers.set("X-USER-ID", "nonExistentUser");
                headers.setContentType(MediaType.APPLICATION_JSON);

                OrderRequest request = new OrderRequest(
                        List.of(
                                new OrderRequest.OrderItemRequest(productId, 2L)
                        )
                );

                HttpEntity<OrderRequest> httpEntity = new HttpEntity<>(request, headers);

                ParameterizedTypeReference<ApiResponse<Void>> responseType =
                        new ParameterizedTypeReference<>() {
                        };

                ResponseEntity<ApiResponse<Void>> response =
                        testRestTemplate.exchange(endPoint, HttpMethod.POST, httpEntity, responseType);

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            }
        }

        @Nested
        @DisplayName("존재하지 않는 상품 ID로 요청할 경우")
        class 존재하지_않는_상품_ID로_요청 {

            @BeforeEach
            void setUp() {
                User user = userRepository.save(
                        User.create(
                                UserIdentifier.create("kilian"),
                                UserEmail.create("kilian@gmail.com"),
                                UserBirthDay.create("1997-10-08"),
                                UserGender.create("MALE")
                        )
                );
                userIdentifier = user.getIdentifier().value();
            }

            @Test
            @DisplayName("404 Not Found 응답을 반환한다.")
            void notFound응답을_반환한다() {
                // When
                String endPoint = "/api/v1/orders";
                HttpHeaders headers = new HttpHeaders();
                headers.set("X-USER-ID", userIdentifier);
                headers.setContentType(MediaType.APPLICATION_JSON);

                OrderRequest request = new OrderRequest(
                        List.of(
                                new OrderRequest.OrderItemRequest("99999", 2L)
                        )
                );

                HttpEntity<OrderRequest> httpEntity = new HttpEntity<>(request, headers);

                ParameterizedTypeReference<ApiResponse<Void>> responseType =
                        new ParameterizedTypeReference<>() {
                        };

                ResponseEntity<ApiResponse<Void>> response =
                        testRestTemplate.exchange(endPoint, HttpMethod.POST, httpEntity, responseType);

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            }
        }
    }
}
