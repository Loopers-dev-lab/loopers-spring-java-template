package com.loopers.application.api.productlike;

import com.loopers.application.api.ApiIntegrationTest;
import com.loopers.application.api.common.dto.ApiResponse;
import com.loopers.core.domain.brand.Brand;
import com.loopers.core.domain.brand.repository.BrandRepository;
import com.loopers.core.domain.brand.vo.BrandDescription;
import com.loopers.core.domain.brand.vo.BrandName;
import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.repository.ProductRepository;
import com.loopers.core.domain.product.vo.ProductId;
import com.loopers.core.domain.product.vo.ProductName;
import com.loopers.core.domain.product.vo.ProductPrice;
import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.repository.UserRepository;
import com.loopers.core.domain.user.type.UserGender;
import com.loopers.core.domain.user.vo.UserBirthDay;
import com.loopers.core.domain.user.vo.UserEmail;
import com.loopers.core.domain.user.vo.UserIdentifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProductLikeV1ApiIntegrationTest extends ApiIntegrationTest {

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected BrandRepository brandRepository;

    @Autowired
    protected ProductRepository productRepository;

    protected String userId;
    protected String productId;
    protected Product product;

    @Nested
    @DisplayName("상품 좋아요 등록")
    class 상품_좋아요_등록 {

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
                userId = user.getIdentifier().value();

                Brand brand = brandRepository.save(
                        Brand.create(new BrandName("kilian"), new BrandDescription("향수 브랜드"))
                );

                product = productRepository.save(
                        Product.create(
                                brand.getId(),
                                new ProductName("엔젤스 쉐어"),
                                new ProductPrice(new BigDecimal("150.00"))
                        )
                );
                productId = product.getId().value();
            }

            @Test
            @DisplayName("상품에 좋아요를 등록한다.")
            void 상품에_좋아요를_등록한다() {
                // When
                String endPoint = "/api/v1/like/products/" + productId;
                HttpHeaders headers = new HttpHeaders();
                headers.set("X-USER-ID", userId);
                HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

                ParameterizedTypeReference<ApiResponse<Void>> responseType =
                        new ParameterizedTypeReference<>() {
                        };

                ResponseEntity<ApiResponse<Void>> response =
                        testRestTemplate.exchange(endPoint, HttpMethod.POST, httpEntity, responseType);

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();

                // 좋아요 수 확인
                Product likedProduct = productRepository.getById(new ProductId(productId));
                assertThat(likedProduct.getLikeCount().value()).isEqualTo(1);
            }
        }

        @Nested
        @DisplayName("X-USER-ID 헤더가 없을 경우")
        class X_USER_ID_헤더가_없을_경우 {

            @Test
            @DisplayName("400 Bad Request 응답을 반환한다.")
            void badRequest응답을_반환한다() {
                // When
                String endPoint = "/api/v1/like/products/" + productId;

                ParameterizedTypeReference<ApiResponse<Void>> responseType =
                        new ParameterizedTypeReference<>() {
                        };

                ResponseEntity<ApiResponse<Void>> response =
                        testRestTemplate.exchange(endPoint, HttpMethod.POST, HttpEntity.EMPTY, responseType);

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
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
                userId = user.getIdentifier().value();
            }

            @Test
            @DisplayName("404 Not Found 응답을 반환한다.")
            void notFound응답을_반환한다() {
                // When
                String endPoint = "/api/v1/like/products/99999";
                HttpHeaders headers = new HttpHeaders();
                headers.set("X-USER-ID", userId);
                HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

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
        @DisplayName("존재하지 않는 사용자로 요청할 경우")
        class 존재하지_않는_사용자로_요청 {

            @Test
            @DisplayName("404 Not Found 응답을 반환한다.")
            void notFound응답을_반환한다() {
                // When
                String endPoint = "/api/v1/like/products/" + productId;
                HttpHeaders headers = new HttpHeaders();
                headers.set("X-USER-ID", "99999");
                HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

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

    @Nested
    @DisplayName("상품 좋아요 취소")
    class 상품_좋아요_취소 {

        @Nested
        @DisplayName("좋아요가 등록되어 있을 경우")
        class 좋아요가_등록되어_있을_경우 {

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
                userId = user.getIdentifier().value();

                Brand brand = brandRepository.save(
                        Brand.create(new BrandName("kilian"), new BrandDescription("향수 브랜드"))
                );

                productId = productRepository.save(
                        Product.create(
                                brand.getId(),
                                new ProductName("엔젤스 쉐어"),
                                new ProductPrice(new BigDecimal("150.00"))
                        )
                ).getId().value();

                // 좋아요 등록
                String endPoint = "/api/v1/like/products/" + productId;
                HttpHeaders headers = new HttpHeaders();
                headers.set("X-USER-ID", userId);
                HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

                ParameterizedTypeReference<ApiResponse<Void>> responseType =
                        new ParameterizedTypeReference<>() {
                        };

                testRestTemplate.exchange(endPoint, HttpMethod.POST, httpEntity, responseType);
            }

            @Test
            @DisplayName("상품의 좋아요를 취소한다.")
            void 상품의_좋아요를_취소한다() {
                // When
                String endPoint = "/api/v1/like/products/" + productId;
                HttpHeaders headers = new HttpHeaders();
                headers.set("X-USER-ID", userId);
                HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

                ParameterizedTypeReference<ApiResponse<Void>> responseType =
                        new ParameterizedTypeReference<>() {
                        };

                ResponseEntity<ApiResponse<Void>> response =
                        testRestTemplate.exchange(endPoint, HttpMethod.DELETE, httpEntity, responseType);

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();

                // 좋아요 수 확인
                Product unlikedProduct = productRepository.getById(new ProductId(productId));
                assertThat(unlikedProduct.getLikeCount().value()).isEqualTo(0);
            }
        }

        @Nested
        @DisplayName("X-USER-ID 헤더가 없을 경우")
        class X_USER_ID_헤더가_없을_경우 {

            @Test
            @DisplayName("400 Bad Request 응답을 반환한다.")
            void badRequest응답을_반환한다() {
                // When
                String endPoint = "/api/v1/like/products/" + productId;

                ParameterizedTypeReference<ApiResponse<Void>> responseType =
                        new ParameterizedTypeReference<>() {
                        };

                ResponseEntity<ApiResponse<Void>> response =
                        testRestTemplate.exchange(endPoint, HttpMethod.DELETE, HttpEntity.EMPTY, responseType);

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
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
                userId = user.getIdentifier().value();
            }

            @Test
            @DisplayName("404 Not Found 응답을 반환한다.")
            void notFound응답을_반환한다() {
                // When
                String endPoint = "/api/v1/like/products/99999";
                HttpHeaders headers = new HttpHeaders();
                headers.set("X-USER-ID", userId);
                HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

                ParameterizedTypeReference<ApiResponse<Void>> responseType =
                        new ParameterizedTypeReference<>() {
                        };

                ResponseEntity<ApiResponse<Void>> response =
                        testRestTemplate.exchange(endPoint, HttpMethod.DELETE, httpEntity, responseType);

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            }
        }

        @Nested
        @DisplayName("존재하지 않는 사용자로 요청할 경우")
        class 존재하지_않는_사용자로_요청 {

            @Test
            @DisplayName("404 Not Found 응답을 반환한다.")
            void notFound응답을_반환한다() {
                // When
                String endPoint = "/api/v1/like/products/" + productId;
                HttpHeaders headers = new HttpHeaders();
                headers.set("X-USER-ID", "nonExistUser");
                HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

                ParameterizedTypeReference<ApiResponse<Void>> responseType =
                        new ParameterizedTypeReference<>() {
                        };

                ResponseEntity<ApiResponse<Void>> response =
                        testRestTemplate.exchange(endPoint, HttpMethod.DELETE, httpEntity, responseType);

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            }
        }
    }
}
