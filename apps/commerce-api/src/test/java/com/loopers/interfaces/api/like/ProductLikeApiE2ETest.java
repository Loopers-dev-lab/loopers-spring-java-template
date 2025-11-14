package com.loopers.interfaces.api.like;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.like.ProductLikeJpaRepository;
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
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductLikeApiE2ETest {

    private static final String ENDPOINT = "/api/v1/like/products";

    private final TestRestTemplate testRestTemplate;
    private final UserJpaRepository userJpaRepository;
    private final BrandJpaRepository brandJpaRepository;
    private final ProductJpaRepository productJpaRepository;
    private final ProductLikeJpaRepository productLikeJpaRepository;
    private final DatabaseCleanUp databaseCleanUp;

    @Autowired
    public ProductLikeApiE2ETest(
            TestRestTemplate testRestTemplate,
            UserJpaRepository userJpaRepository,
            BrandJpaRepository brandJpaRepository,
            ProductJpaRepository productJpaRepository,
            ProductLikeJpaRepository productLikeJpaRepository,
            DatabaseCleanUp databaseCleanUp
    ) {
        this.testRestTemplate = testRestTemplate;
        this.userJpaRepository = userJpaRepository;
        this.brandJpaRepository = brandJpaRepository;
        this.productJpaRepository = productJpaRepository;
        this.productLikeJpaRepository = productLikeJpaRepository;
        this.databaseCleanUp = databaseCleanUp;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("POST /api/v1/like/products/{productId}")
    @Nested
    class LikeProduct {

        @DisplayName("로그인한 사용자가 상품에 좋아요를 등록할 수 있다.")
        @Test
        void likeTest1() {
            // arrange
            User user = userJpaRepository.save(
                    User.create("user123", "user@test.com", "2000-01-01", Gender.MALE)
            );
            Brand brand = brandJpaRepository.save(Brand.create("브랜드A"));
            Product product = productJpaRepository.save(
                    Product.create("상품A", "설명", 10_000, 100L, brand.getId())
            );

            String url = ENDPOINT + "/" + product.getId();

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", user.getUserId());

            HttpEntity<Void> request = new HttpEntity<>(headers);

            // act
            ParameterizedTypeReference<ApiResponse<Void>> type =
                    new ParameterizedTypeReference<>() {
                    };

            ResponseEntity<ApiResponse<Void>> response =
                    testRestTemplate.exchange(url, HttpMethod.POST, request, type);

            // assert
            assertAll(
                    () -> assertThat(response.getStatusCode().is2xxSuccessful()).isTrue(),
                    () -> {
                        // 좋아요 생성 확인
                        long likeCount = productLikeJpaRepository.count();
                        assertThat(likeCount).isEqualTo(1L);
                    },
                    () -> {
                        // totalLikes 증가 확인
                        Product updatedProduct = productJpaRepository.findById(product.getId()).get();
                        assertThat(updatedProduct.getTotalLikes()).isEqualTo(1L);
                    }
            );
        }

        @DisplayName("동일한 상품에 여러 번 좋아요를 요청해도 한 번만 등록된다.")
        @Test
        void likeTest2() {
            // arrange
            User user = userJpaRepository.save(
                    User.create("user123", "user@test.com", "2000-01-01", Gender.MALE)
            );
            Brand brand = brandJpaRepository.save(Brand.create("브랜드A"));
            Product product = productJpaRepository.save(
                    Product.create("상품A", "설명", 10_000, 100L, brand.getId())
            );

            String url = ENDPOINT + "/" + product.getId();

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", user.getUserId());

            HttpEntity<Void> request = new HttpEntity<>(headers);

            // act
            ParameterizedTypeReference<ApiResponse<ProductLikeDto.LikeResponse>> type =
                    new ParameterizedTypeReference<>() {
                    };

            testRestTemplate.exchange(url, HttpMethod.POST, request, type);
            testRestTemplate.exchange(url, HttpMethod.POST, request, type);
            ResponseEntity<ApiResponse<ProductLikeDto.LikeResponse>> response =
                    testRestTemplate.exchange(url, HttpMethod.POST, request, type);

            assertAll(
                    () -> assertThat(response.getStatusCode().is2xxSuccessful()).isTrue(),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data().liked()).isTrue(),
                    () -> assertThat(response.getBody().data().totalLikes()).isEqualTo(1L),
                    () -> {
                        long likeCount = productLikeJpaRepository.count();
                        assertThat(likeCount).isEqualTo(1L);
                    },
                    () -> {
                        Product updatedProduct = productJpaRepository.findById(product.getId()).get();
                        assertThat(updatedProduct.getTotalLikes()).isEqualTo(1L);
                    }
            );
        }

        @DisplayName("존재하지 않는 사용자로 요청할 경우, 404 에러를 반환한다.")
        @Test
        void likeTest3() {
            // arrange
            Brand brand = brandJpaRepository.save(Brand.create("브랜드A"));
            Product product = productJpaRepository.save(
                    Product.create("상품A", "설명", 10_000, 100L, brand.getId())
            );

            String url = ENDPOINT + "/" + product.getId();

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", "user123");

            HttpEntity<Void> request = new HttpEntity<>(headers);

            // act
            ParameterizedTypeReference<ApiResponse<Object>> type =
                    new ParameterizedTypeReference<>() {
                    };

            ResponseEntity<ApiResponse<Object>> response =
                    testRestTemplate.exchange(url, HttpMethod.POST, request, type);

            // assert
            assertAll(
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().meta().message())
                            .contains("해당 사용자를 찾을 수 없습니다")
            );
        }

        @DisplayName("존재하지 않는 상품으로 요청할 경우, 404 에러를 반환한다.")
        @Test
        void likeTest4() {
            // arrange
            User user = userJpaRepository.save(
                    User.create("user123", "user@test.com", "2000-01-01", Gender.MALE)
            );

            String url = ENDPOINT + "/999999";

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", user.getUserId());

            HttpEntity<Void> request = new HttpEntity<>(headers);

            //act
            ParameterizedTypeReference<ApiResponse<Object>> type =
                    new ParameterizedTypeReference<>() {
                    };

            ResponseEntity<ApiResponse<Object>> response =
                    testRestTemplate.exchange(url, HttpMethod.POST, request, type);

            // assert
            assertAll(
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().meta().message())
                            .contains("해당 상품을 찾을 수 없습니다")
            );
        }
    }

    @DisplayName("DELETE /api/v1/like/products/{productId}")
    @Nested
    class UnlikeProduct {

        @DisplayName("로그인한 사용자가 상품 좋아요를 취소할 수 있다.")
        @Test
        void unlikeTest1() {
            // arrange
            User user = userJpaRepository.save(
                    User.create("user123", "user@test.com", "2000-01-01", Gender.MALE)
            );
            Brand brand = brandJpaRepository.save(Brand.create("브랜드A"));
            Product product = productJpaRepository.save(
                    Product.create("상품A", "설명", 10_000, 100L, brand.getId())
            );

            // 좋아요 등록
            String url = ENDPOINT + "/" + product.getId();
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", user.getUserId());
            HttpEntity<Void> request = new HttpEntity<>(headers);
            ParameterizedTypeReference<ApiResponse<ProductLikeDto.LikeResponse>> likeType =
                    new ParameterizedTypeReference<>() {
                    };
            testRestTemplate.exchange(url, HttpMethod.POST, request, likeType);

            // act
            // 좋아요 취소
            ParameterizedTypeReference<ApiResponse<ProductLikeDto.LikeResponse>> type =
                    new ParameterizedTypeReference<>() {
                    };

            ResponseEntity<ApiResponse<ProductLikeDto.LikeResponse>> response =
                    testRestTemplate.exchange(url, HttpMethod.DELETE, request, type);

            // assert
            assertAll(
                    () -> assertThat(response.getStatusCode().is2xxSuccessful()).isTrue(),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data().liked()).isFalse(),
                    () -> assertThat(response.getBody().data().totalLikes()).isEqualTo(0L),
                    () -> {
                        long likeCount = productLikeJpaRepository.count();
                        assertThat(likeCount).isEqualTo(1L);  // 소프트 삭제라서 존재
                    },
                    () -> {
                        Product updatedProduct = productJpaRepository.findById(product.getId()).get();
                        assertThat(updatedProduct.getTotalLikes()).isEqualTo(0L);
                    }
            );
        }

        @DisplayName("좋아요하지 않은 상품을 여러 번 취소해도 동일한 상태가 유지된다.")
        @Test
        void unlikeTest2() {
            // arrange
            User user = userJpaRepository.save(
                    User.create("user123", "user@test.com", "2000-01-01", Gender.MALE)
            );
            Brand brand = brandJpaRepository.save(Brand.create("브랜드A"));
            Product product = productJpaRepository.save(
                    Product.create("상품A", "설명", 10_000, 100L, brand.getId())
            );

            String url = ENDPOINT + "/" + product.getId();

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", user.getUserId());

            HttpEntity<Void> request = new HttpEntity<>(headers);

            //act
            ParameterizedTypeReference<ApiResponse<ProductLikeDto.LikeResponse>> type =
                    new ParameterizedTypeReference<>() {
                    };

            testRestTemplate.exchange(url, HttpMethod.DELETE, request, type);
            testRestTemplate.exchange(url, HttpMethod.DELETE, request, type);
            ResponseEntity<ApiResponse<ProductLikeDto.LikeResponse>> response =
                    testRestTemplate.exchange(url, HttpMethod.DELETE, request, type);

            // assert
            assertAll(
                    () -> assertThat(response.getStatusCode().is2xxSuccessful()).isTrue(),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data().liked()).isFalse(),
                    () -> assertThat(response.getBody().data().totalLikes()).isEqualTo(0L),
                    () -> {
                        long likeCount = productLikeJpaRepository.count();
                        assertThat(likeCount).isEqualTo(0L);
                    },
                    () -> {
                        Product updatedProduct = productJpaRepository.findById(product.getId()).get();
                        assertThat(updatedProduct.getTotalLikes()).isEqualTo(0L);
                    }
            );
        }
    }

    @DisplayName("GET /api/v1/like/products")
    @Nested
    class GetLikedProducts {
        @DisplayName("로그인한 사용자가 좋아요한 상품 목록을 조회할 수 있다.")
        @Test
        void getLikedProductsTest1() {
            // arrange
            User user = userJpaRepository.save(
                    User.create("user123", "user@test.com", "2000-01-01", Gender.MALE)
            );

            Brand brandA = brandJpaRepository.save(Brand.create("브랜드A"));
            Brand brandB = brandJpaRepository.save(Brand.create("브랜드B"));

            Product product1 = productJpaRepository.save(
                    Product.create("상품1", "설명1", 10_000, 100L, brandA.getId())
            );
            Product product2 = productJpaRepository.save(
                    Product.create("상품2", "설명2", 20_000, 50L, brandB.getId())
            );
            Product product3 = productJpaRepository.save(
                    Product.create("상품3", "설명3", 30_000, 30L, brandA.getId())
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", user.getUserId());
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ParameterizedTypeReference<ApiResponse<ProductLikeDto.LikeResponse>> likeType =
                    new ParameterizedTypeReference<>() {};
            testRestTemplate.exchange(ENDPOINT + "/" + product1.getId(), HttpMethod.POST, request, likeType);
            testRestTemplate.exchange(ENDPOINT + "/" + product2.getId(), HttpMethod.POST, request, likeType);

            // act
            ParameterizedTypeReference<ApiResponse<ProductLikeDto.LikedProductsResponse>> type =
                    new ParameterizedTypeReference<>() {};

            ResponseEntity<ApiResponse<ProductLikeDto.LikedProductsResponse>> response =
                    testRestTemplate.exchange(ENDPOINT, HttpMethod.GET, request, type);

            // assert
            assertAll(
                    () -> assertThat(response.getStatusCode().is2xxSuccessful()).isTrue(),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data().totalCount()).isEqualTo(2),
                    () -> assertThat(response.getBody().data().products()).hasSize(2),
                    () -> {
                        // 상품 ID 확인
                        var productIds = response.getBody().data().products().stream()
                                .map(ProductLikeDto.ProductSummary::id)
                                .toList();
                        assertThat(productIds).containsExactlyInAnyOrder(product1.getId(), product2.getId());
                    },
                    () -> {
                        // 상품 정보 확인
                        var product1Summary = response.getBody().data().products().stream()
                                .filter(p -> p.id().equals(product1.getId()))
                                .findFirst()
                                .orElseThrow();

                        assertThat(product1Summary.name()).isEqualTo("상품1");
                        assertThat(product1Summary.price()).isEqualTo(10_000);
                        assertThat(product1Summary.totalLikes()).isEqualTo(1L);
                    }
            );
        }
    }
}
