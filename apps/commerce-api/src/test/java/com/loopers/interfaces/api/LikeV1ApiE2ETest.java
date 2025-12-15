package com.loopers.interfaces.api;

import com.loopers.application.like.LikeFacade;
import com.loopers.domain.common.vo.Money;
import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductLikeCount;
import com.loopers.domain.product.ProductLikeCountRepository;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import com.loopers.interfaces.api.like.LikeV1Dto;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static com.loopers.support.fixture.UserFixtures.createValidUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LikeV1ApiE2ETest {

    private static final Function<Long, String> ENDPOINT_PO_DE = userId -> "/api/v1/like/products/" + userId;
    private static final String ENDPOINT_GET = "/api/v1/like/products";

    private final TestRestTemplate testRestTemplate;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductLikeCountRepository productLikeCountRepository;
    private final LikeRepository likeRepository;
    private final LikeFacade likeFacade;
    private final DatabaseCleanUp databaseCleanUp;

    private User testUser;
    private Product testProduct;

    @Autowired
    public LikeV1ApiE2ETest(
            TestRestTemplate testRestTemplate,
            UserRepository userRepository,
            ProductRepository productRepository,
            ProductLikeCountRepository productLikeCountRepository,
            LikeRepository likeRepository,
            LikeFacade likeFacade,
            DatabaseCleanUp databaseCleanUp
    ) {
        this.testRestTemplate = testRestTemplate;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.productLikeCountRepository = productLikeCountRepository;
        this.likeRepository = likeRepository;
        this.likeFacade = likeFacade;
        this.databaseCleanUp = databaseCleanUp;
    }

    @BeforeEach
    void setUp() {
        databaseCleanUp.truncateAllTables(); // 각 테스트 전에 테이블 초기화
        testUser = userRepository.save(createValidUser());
        testProduct = productRepository.save(Product.create(1L, "테스트상품1", Money.of(1000)));
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    private HttpHeaders createHeaders(Long userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-USER-ID", String.valueOf(userId));
        return headers;
    }

    @DisplayName("POST /api/v1/like/products/{productId}")
    @Nested
    class AddLike {
        @DisplayName("좋아요 등록 시 좋아요가 정상 등록되고 집계 카운트가 1 증가한다.")
        @Test
        void returnsOk_whenAddLikeSucceeds() {
            // arrange
            HttpHeaders headers = createHeaders(testUser.getId());
            String requestUrl = ENDPOINT_PO_DE.apply(testProduct.getId());

            // act
            ResponseEntity<Void> response = testRestTemplate.exchange(
                    requestUrl, HttpMethod.POST, new HttpEntity<>(headers), Void.class);

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
                Optional<Like> foundLike = likeRepository.findByUserIdAndProductId(testUser.getId(), testProduct.getId());
                Optional<ProductLikeCount> productLikeCount = productLikeCountRepository.findById(testProduct.getId());

                assertAll(
                        () -> assertThat(foundLike).isPresent(),
                        () -> assertThat(productLikeCount).isPresent(),
                        () -> assertThat(productLikeCount.get().getLikeCount()).isEqualTo(1)
                );
            });
        }

        @DisplayName("이미 좋아요한 상품에 중복 등록 시도 시 오류 없이 멱등성을 유지한다.")
        @Test
        void returnsBadRequest_whenAddDuplicateLike() {
            // arrange
            HttpHeaders headers = createHeaders(testUser.getId());
            String requestUrl = ENDPOINT_PO_DE.apply(testProduct.getId());

            likeFacade.addLike(testUser.getId(), testProduct.getId());

            // act
            ResponseEntity<Void> response = testRestTemplate.exchange(
                    requestUrl, HttpMethod.POST, new HttpEntity<>(headers), Void.class);

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
                Optional<Like> foundLike = likeRepository.findByUserIdAndProductId(testUser.getId(), testProduct.getId());
                Optional<ProductLikeCount> productLikeCount = productLikeCountRepository.findById(testProduct.getId());

                assertAll(
                        () -> assertThat(foundLike).isPresent(),
                        () -> assertThat(productLikeCount).isPresent(),
                        () -> assertThat(productLikeCount.get().getLikeCount()).isEqualTo(1)
                );
            });
        }

        @DisplayName("존재하지 않는 상품을 좋아요 등록 시도 시 NOT FOUND를 리턴한다.")
        @Test
        void shouldReturnNotFoundForInvalidProduct() throws Exception {
            // Act & Assert
            HttpHeaders headers = createHeaders(testUser.getId());
            String requestUrl = ENDPOINT_PO_DE.apply(-1L);

            ResponseEntity<ApiResponse<Void>> response = testRestTemplate.exchange(
                    requestUrl, HttpMethod.POST, new HttpEntity<>(headers), new ParameterizedTypeReference<>() {});

            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
            );
        }
    }

    @DisplayName("DELETE /api/v1/like/products/{productId}")
    @Nested
    class RemoveLike {
        @DisplayName("좋아요 한 상품을 좋아요 취소 시 정상적으로 취소되고, 집계 카운트도 1 감소한다.")
        @Test
        void returnsOk_whenRemoveLikeSucceeds() throws Exception {
            // arrange
            HttpHeaders headers = createHeaders(testUser.getId());
            String requestUrl = ENDPOINT_PO_DE.apply(testProduct.getId());

            likeFacade.addLike(testUser.getId(), testProduct.getId());

            // act
            ResponseEntity<Void> response = testRestTemplate.exchange(
                    requestUrl, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
                Optional<Like> foundLike = likeRepository.findByUserIdAndProductId(testUser.getId(), testProduct.getId());
                Optional<ProductLikeCount> productLikeCount = productLikeCountRepository.findById(testProduct.getId());

                assertAll(
                        () -> assertThat(foundLike).isEmpty(),
                        () -> assertThat(productLikeCount).isPresent(),
                        () -> assertThat(productLikeCount.get().getLikeCount()).isEqualTo(0)
                );
            });
        }

        @DisplayName("좋아요 취소한 상품을 중복 취소 시 멱등성을 유지한다.")
        @Test
        void returnsBadRequest_whenRemoveNonExistentLike() throws InterruptedException {
            // arrange
            HttpHeaders headers = createHeaders(testUser.getId());
            String requestUrl = ENDPOINT_PO_DE.apply(testProduct.getId());

            likeFacade.addLike(testUser.getId(), testProduct.getId());

            Thread.sleep(2000);
            likeFacade.removeLike(testUser.getId(), testProduct.getId());
            Thread.sleep(2000);

            // act
            ResponseEntity<Void> response = testRestTemplate.exchange(
                    requestUrl, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
                Optional<Like> foundLike = likeRepository.findByUserIdAndProductId(testUser.getId(), testProduct.getId());
                Optional<ProductLikeCount> productLikeCount = productLikeCountRepository.findById(testProduct.getId());

                assertAll(
                        () -> assertThat(foundLike).isEmpty(),
                        () -> assertThat(productLikeCount).isPresent(),
                        () -> assertThat(productLikeCount.get().getLikeCount()).isEqualTo(0)
                );
            });
        }
    }

    @DisplayName("GET /api/v1/like/products")
    @Nested
    class GetLikedProducts {
        @DisplayName("좋아요한 상품 목록 조회 성공 시 200 OK 응답과 좋아요한 상품 목록을 반환한다.")
        @Test
        void returnsOkWithLikedProducts_whenGetLikedProductsSucceeds() throws InterruptedException {
            // arrange
            Product anotherProduct = productRepository.save(Product.create(2L, "테스트상품2", Money.of(2000)));
            likeFacade.addLike(testUser.getId(), testProduct.getId());
            likeFacade.addLike(testUser.getId(), anotherProduct.getId());
            Thread.sleep(2000);

            HttpHeaders headers = createHeaders(testUser.getId());

            // act
            ResponseEntity<ApiResponse<List<LikeV1Dto.LikedProductResponse>>> response = testRestTemplate.exchange(
                    ENDPOINT_GET, HttpMethod.GET, new HttpEntity<>(headers), new ParameterizedTypeReference<>() {});

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().data()).hasSize(2);

            List<LikeV1Dto.LikedProductResponse> likedProducts = response.getBody().data();
            assertAll(
                    () -> assertThat(likedProducts).extracting(LikeV1Dto.LikedProductResponse::productId)
                            .containsExactlyInAnyOrder(testProduct.getId(), anotherProduct.getId()),
                    () -> assertThat(likedProducts).allMatch(p -> p.likeCount() == 1)
            );
        }

        @DisplayName("좋아요한 상품이 없는 경우 빈 목록을 반환한다.")
        @Test
        void returnsOkWithEmptyList_whenNoLikedProducts() {
            // arrange
            HttpHeaders headers = createHeaders(testUser.getId());

            // act
            ResponseEntity<ApiResponse<List<LikeV1Dto.LikedProductResponse>>> response = testRestTemplate.exchange(
                    ENDPOINT_GET, HttpMethod.GET, new HttpEntity<>(headers), new ParameterizedTypeReference<>() {});

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().data()).isEmpty();
        }
    }
}
