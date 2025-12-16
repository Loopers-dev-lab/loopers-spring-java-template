package com.loopers.interfaces.api.like;

import com.loopers.domain.Money;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.infrastructure.brand.BrandJpaRepository;
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductLikeV1ControllerE2ETest {

    private final TestRestTemplate testRestTemplate;

    private final UserJpaRepository userJpaRepository;

    private final BrandJpaRepository brandJpaRepository;

    private final ProductJpaRepository productJpaRepository;

    private final DatabaseCleanUp databaseCleanUp;

    @Autowired
    public ProductLikeV1ControllerE2ETest(
                TestRestTemplate testRestTemplate,
                UserJpaRepository userJpaRepository,
                BrandJpaRepository brandJpaRepository,
                ProductJpaRepository productJpaRepository,
                DatabaseCleanUp databaseCleanUp
    ) {
        this.testRestTemplate = testRestTemplate;
        this.userJpaRepository = userJpaRepository;
        this.brandJpaRepository = brandJpaRepository;
        this.productJpaRepository = productJpaRepository;
        this.databaseCleanUp = databaseCleanUp;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("POST /api/v1/product-likes")
    @Nested
    class AddLike {
        @DisplayName("상품에 대한 좋아요 처리에 성공할 경우 상품 좋아요 정보를 반환한다.")
        @Test
        void addLikeSuccess_returnProductLikeInfo() {

            // given
            String userId = "test123";

            User user = User.builder()
                    .userId(userId)
                    .email("test@test.com")
                    .birthdate("1995-08-25")
                    .gender(Gender.MALE)
                    .build();

            User savedUser = userJpaRepository.save(user);

            Brand brand = Brand.createBrand("나이키");
            Brand savedBrand = brandJpaRepository.save(brand);

            Product product = Product.createProduct(
                    "P001",
                    "에어맥스",
                    Money.of(150000),
                    50,
                    savedBrand
            );
            Product savedProduct = productJpaRepository.save(product);

            // when

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", userId);

            HttpEntity<Long> httpEntity = new HttpEntity<>(savedProduct.getId(), headers);

            ParameterizedTypeReference<ApiResponse<ProductLikeV1Dto.ProductLikeResponse>> responseType
                                                                            = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<ProductLikeV1Dto.ProductLikeResponse>> response =
                    testRestTemplate.exchange(
                            "/api/v1/product-likes/new",
                            HttpMethod.POST,
                            httpEntity,
                            responseType
                    );

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data().productIdx()).isEqualTo(savedProduct.getId()),
                    () -> assertThat(response.getBody().data().likeUserIdx()).isEqualTo(savedUser.getId())
            );

        }

        @DisplayName("동일한 상품에 대해 여러 유저가 동시에 좋아요를 요청해도 좋아요 개수가 정상적으로 반영된다.")
        @Test
        void addLike_withConcurrentRequests_incrementsLikeCountCorrectly() {
            // given
            Brand brand = Brand.createBrand("나이키");
            Brand savedBrand = brandJpaRepository.save(brand);

            Product product = Product.createProduct(
                    "P001",
                    "에어맥스",
                    Money.of(150000),
                    50,
                    savedBrand
            );
            Product savedProduct = productJpaRepository.save(product);

            // 10명의 유저 생성
            int numberOfUsers = 10;
            List<User> users = new ArrayList<>();
            for (int i = 0; i < numberOfUsers; i++) {
                User user = User.builder()
                        .userId("testuser" + i)
                        .email("test" + i + "@test.com")
                        .birthdate("1995-08-25")
                        .gender(Gender.MALE)
                        .build();
                users.add(userJpaRepository.save(user));
            }

            ExecutorService executorService = Executors.newFixedThreadPool(numberOfUsers);

            // when - 10명이 동시에 좋아요 요청
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (User user : users) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    HttpHeaders headers = new HttpHeaders();
                    headers.set("X-USER-ID", user.getUserId());

                    HttpEntity<Long> httpEntity = new HttpEntity<>(savedProduct.getId(), headers);

                    testRestTemplate.exchange(
                            "/api/v1/product-likes/new",
                            HttpMethod.POST,
                            httpEntity,
                            new ParameterizedTypeReference<ApiResponse<ProductLikeV1Dto.ProductLikeResponse>>() {}
                    );
                }, executorService);
                futures.add(future);
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            executorService.shutdown();

            // then - 좋아요 개수가 정확히 10개 증가했는지 확인
            await().atMost(Duration.ofSeconds(5))
                    .pollInterval(Duration.ofMillis(300))
                    .untilAsserted(() -> {
                        Product finalProduct = productJpaRepository.findById(savedProduct.getId()).orElseThrow();
                        assertThat(finalProduct.getLikeCount()).isEqualTo(numberOfUsers);
                    });

        }
    }
}
