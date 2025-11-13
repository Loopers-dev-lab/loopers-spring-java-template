package com.loopers.interfaces.api.like;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.infrastructure.like.ProductLikeJpaRepository;
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
                    new ParameterizedTypeReference<>() {};

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
                    new ParameterizedTypeReference<>() {};

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
    }
}
