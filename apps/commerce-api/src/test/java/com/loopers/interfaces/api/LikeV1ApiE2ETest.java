package com.loopers.interfaces.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.util.Objects;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserRegisterCommand;
import com.loopers.domain.brand.BrandEntity;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.product.ProductDomainCreateRequest;
import com.loopers.domain.product.ProductEntity;
import com.loopers.domain.product.ProductService;
import com.loopers.fixtures.BrandTestFixture;
import com.loopers.fixtures.ProductTestFixture;
import com.loopers.fixtures.UserTestFixture;
import com.loopers.interfaces.api.like.LikeV1Dtos;
import com.loopers.interfaces.api.product.ProductV1Dtos;
import com.loopers.support.Uris;
import com.loopers.utils.DatabaseCleanUp;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Like API E2E 테스트")
class LikeV1ApiE2ETest {

    private final TestRestTemplate testRestTemplate;
    private final DatabaseCleanUp databaseCleanUp;

    @Autowired
    private ProductService productService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private UserFacade userFacade;

    @Autowired
    public LikeV1ApiE2ETest(
            TestRestTemplate testRestTemplate,
            DatabaseCleanUp databaseCleanUp
    ) {
        this.testRestTemplate = testRestTemplate;
        this.databaseCleanUp = databaseCleanUp;
    }

    private String testUsername;
    private Long testProductId;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성
        UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
        userFacade.registerUser(userCommand);
        testUsername = userCommand.username();

        // 테스트용 브랜드 생성
        BrandEntity brand = brandService.registerBrand(
                BrandTestFixture.createRequest("테스트브랜드", "E2E 테스트용 브랜드")
        );

        // 테스트용 상품 생성
        ProductDomainCreateRequest productRequest = ProductTestFixture.createRequest(
                brand.getId(),
                "테스트상품",
                "E2E 테스트용 상품",
                new BigDecimal("10000"),
                100
        );
        ProductEntity product = productService.registerProduct(productRequest);
        testProductId = product.getId();
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Nested
    @DisplayName("좋아요 등록 및 조회 플로우")
    class LikeRegistrationAndRetrievalFlow {

        @Test
        @DisplayName("좋아요 등록 후 상품 상세 조회 시 좋아요 여부가 true로 표시된다")
        void like_product_and_verify_liked_status_in_product_detail() {
            // given
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", testUsername);

            // when - 1. 좋아요 등록
            ParameterizedTypeReference<ApiResponse<LikeV1Dtos.LikeResponse>> likeResponseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<LikeV1Dtos.LikeResponse>> likeResponse =
                    testRestTemplate.exchange(
                            Uris.Like.UPSERT,
                            HttpMethod.POST,
                            new HttpEntity<>(null, headers),
                            likeResponseType,
                            testProductId
                    );

            // then - 1. 좋아요 등록 성공 검증
            assertAll(
                    () -> assertTrue(likeResponse.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(likeResponse.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(Objects.requireNonNull(likeResponse.getBody()).data().username())
                            .isEqualTo(testUsername),
                    () -> assertThat(Objects.requireNonNull(likeResponse.getBody()).data().productId())
                            .isEqualTo(testProductId)
            );

            // when - 2. 상품 상세 조회 (좋아요 여부 확인)
            ParameterizedTypeReference<ApiResponse<ProductV1Dtos.ProductDetailResponse>> productResponseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<ProductV1Dtos.ProductDetailResponse>> productResponse =
                    testRestTemplate.exchange(
                            Uris.Product.GET_DETAIL,
                            HttpMethod.GET,
                            new HttpEntity<>(null, headers),
                            productResponseType,
                            testProductId
                    );

            // then - 2. 좋아요 여부가 true로 표시되는지 검증
            assertAll(
                    () -> assertTrue(productResponse.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(productResponse.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(Objects.requireNonNull(productResponse.getBody()).data().productId())
                            .isEqualTo(testProductId),
                    () -> assertThat(Objects.requireNonNull(productResponse.getBody()).data().isLiked())
                            .isTrue(),
                    () -> assertThat(Objects.requireNonNull(productResponse.getBody()).data().likeCount())
                            .isEqualTo(1L)
            );
        }

        @Test
        @DisplayName("좋아요 등록 후 취소하면 상품 상세 조회 시 좋아요 여부가 false로 표시된다")
        void unlike_product_and_verify_unliked_status_in_product_detail() {
            // given
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", testUsername);

            // when - 1. 좋아요 등록
            ParameterizedTypeReference<ApiResponse<LikeV1Dtos.LikeResponse>> likeResponseType =
                    new ParameterizedTypeReference<>() {
                    };
            testRestTemplate.exchange(
                    Uris.Like.UPSERT,
                    HttpMethod.POST,
                    new HttpEntity<>(null, headers),
                    likeResponseType,
                    testProductId
            );

            // when - 2. 좋아요 취소
            ParameterizedTypeReference<ApiResponse<Void>> unlikeResponseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<Void>> unlikeResponse =
                    testRestTemplate.exchange(
                            Uris.Like.CANCEL,
                            HttpMethod.DELETE,
                            new HttpEntity<>(null, headers),
                            unlikeResponseType,
                            testProductId
                    );

            // then - 2. 좋아요 취소 성공 검증
            assertAll(
                    () -> assertTrue(unlikeResponse.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(unlikeResponse.getStatusCode()).isEqualTo(HttpStatus.OK)
            );

            // when - 3. 상품 상세 조회 (좋아요 여부 확인)
            ParameterizedTypeReference<ApiResponse<ProductV1Dtos.ProductDetailResponse>> productResponseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<ProductV1Dtos.ProductDetailResponse>> productResponse =
                    testRestTemplate.exchange(
                            Uris.Product.GET_DETAIL,
                            HttpMethod.GET,
                            new HttpEntity<>(null, headers),
                            productResponseType,
                            testProductId
                    );

            // then - 3. 좋아요 여부가 false로 표시되는지 검증
            assertAll(
                    () -> assertTrue(productResponse.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(productResponse.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(Objects.requireNonNull(productResponse.getBody()).data().productId())
                            .isEqualTo(testProductId),
                    () -> assertThat(Objects.requireNonNull(productResponse.getBody()).data().isLiked())
                            .isFalse(),
                    () -> assertThat(Objects.requireNonNull(productResponse.getBody()).data().likeCount())
                            .isEqualTo(0L)
            );
        }

        @Test
        @DisplayName("비로그인 사용자는 상품 조회 시 좋아요 여부가 false로 표시된다")
        void guest_user_sees_liked_status_as_false() {
            // given - 로그인한 사용자가 좋아요 등록
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", testUsername);

            ParameterizedTypeReference<ApiResponse<LikeV1Dtos.LikeResponse>> likeResponseType =
                    new ParameterizedTypeReference<>() {
                    };
            testRestTemplate.exchange(
                    Uris.Like.UPSERT,
                    HttpMethod.POST,
                    new HttpEntity<>(null, headers),
                    likeResponseType,
                    testProductId
            );

            // when - 비로그인 사용자가 상품 상세 조회 (헤더 없음)
            ParameterizedTypeReference<ApiResponse<ProductV1Dtos.ProductDetailResponse>> productResponseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<ProductV1Dtos.ProductDetailResponse>> productResponse =
                    testRestTemplate.exchange(
                            Uris.Product.GET_DETAIL,
                            HttpMethod.GET,
                            null,
                            productResponseType,
                            testProductId
                    );

            // then - 비로그인 사용자는 좋아요 여부가 false
            assertAll(
                    () -> assertTrue(productResponse.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(Objects.requireNonNull(productResponse.getBody()).data().isLiked())
                            .isFalse(),
                    () -> assertThat(Objects.requireNonNull(productResponse.getBody()).data().likeCount())
                            .isEqualTo(1L) // 다른 사용자가 좋아요 했으므로 카운트는 1
            );
        }

        @Test
        @DisplayName("중복 좋아요 등록 시 좋아요 수는 증가하지 않는다")
        void duplicate_like_does_not_increase_like_count() {
            // given
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", testUsername);

            // when - 1. 첫 번째 좋아요 등록
            ParameterizedTypeReference<ApiResponse<LikeV1Dtos.LikeResponse>> likeResponseType =
                    new ParameterizedTypeReference<>() {
                    };
            testRestTemplate.exchange(
                    Uris.Like.UPSERT,
                    HttpMethod.POST,
                    new HttpEntity<>(null, headers),
                    likeResponseType,
                    testProductId
            );

            // when - 2. 두 번째 좋아요 등록 (중복)
            testRestTemplate.exchange(
                    Uris.Like.UPSERT,
                    HttpMethod.POST,
                    new HttpEntity<>(null, headers),
                    likeResponseType,
                    testProductId
            );

            // when - 3. 상품 상세 조회
            ParameterizedTypeReference<ApiResponse<ProductV1Dtos.ProductDetailResponse>> productResponseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<ProductV1Dtos.ProductDetailResponse>> productResponse =
                    testRestTemplate.exchange(
                            Uris.Product.GET_DETAIL,
                            HttpMethod.GET,
                            new HttpEntity<>(null, headers),
                            productResponseType,
                            testProductId
                    );

            // then - 좋아요 수는 1개만 카운트
            assertAll(
                    () -> assertTrue(productResponse.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(Objects.requireNonNull(productResponse.getBody()).data().likeCount())
                            .isEqualTo(1L),
                    () -> assertThat(Objects.requireNonNull(productResponse.getBody()).data().isLiked())
                            .isTrue()
            );
        }
    }

    @Nested
    @DisplayName("좋아요 등록 API")
    class UpsertLikeTest {

        @Test
        @DisplayName("유효한 사용자와 상품으로 좋아요 등록에 성공한다")
        void upsert_like_success_with_valid_user_and_product() {
            // given
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", testUsername);

            // when
            ParameterizedTypeReference<ApiResponse<LikeV1Dtos.LikeResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<LikeV1Dtos.LikeResponse>> response =
                    testRestTemplate.exchange(
                            Uris.Like.UPSERT,
                            HttpMethod.POST,
                            new HttpEntity<>(null, headers),
                            responseType,
                            testProductId
                    );

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().username())
                            .isEqualTo(testUsername),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().productId())
                            .isEqualTo(testProductId),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().productName())
                            .isEqualTo("테스트상품")
            );
        }

        @Test
        @DisplayName("존재하지 않는 사용자로 좋아요 등록 시 404를 응답한다")
        void upsert_like_fail_when_user_not_found() {
            // given
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", "nonExistentUser");

            // when
            ParameterizedTypeReference<ApiResponse<LikeV1Dtos.LikeResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<LikeV1Dtos.LikeResponse>> response =
                    testRestTemplate.exchange(
                            Uris.Like.UPSERT,
                            HttpMethod.POST,
                            new HttpEntity<>(null, headers),
                            responseType,
                            testProductId
                    );

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
            );
        }

        @Test
        @DisplayName("존재하지 않는 상품으로 좋아요 등록 시 404를 응답한다")
        void upsert_like_fail_when_product_not_found() {
            // given
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", testUsername);
            Long nonExistentProductId = 99999L;

            // when
            ParameterizedTypeReference<ApiResponse<LikeV1Dtos.LikeResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<LikeV1Dtos.LikeResponse>> response =
                    testRestTemplate.exchange(
                            Uris.Like.UPSERT,
                            HttpMethod.POST,
                            new HttpEntity<>(null, headers),
                            responseType,
                            nonExistentProductId
                    );

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
            );
        }
    }

    @Nested
    @DisplayName("좋아요 취소 API")
    class UnlikeProductTest {

        @Test
        @DisplayName("좋아요한 상품을 취소하면 성공한다")
        void unlike_product_success() {
            // given - 좋아요 등록
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", testUsername);

            ParameterizedTypeReference<ApiResponse<LikeV1Dtos.LikeResponse>> likeResponseType =
                    new ParameterizedTypeReference<>() {
                    };
            testRestTemplate.exchange(
                    Uris.Like.UPSERT,
                    HttpMethod.POST,
                    new HttpEntity<>(null, headers),
                    likeResponseType,
                    testProductId
            );

            // when - 좋아요 취소
            ParameterizedTypeReference<ApiResponse<Void>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<Void>> response =
                    testRestTemplate.exchange(
                            Uris.Like.CANCEL,
                            HttpMethod.DELETE,
                            new HttpEntity<>(null, headers),
                            responseType,
                            testProductId
                    );

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK)
            );
        }

        @Test
        @DisplayName("좋아요하지 않은 상품을 취소해도 성공한다 (멱등성)")
        void unlike_product_success_even_when_not_liked() {
            // given
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", testUsername);

            // when - 좋아요 없이 취소 시도
            ParameterizedTypeReference<ApiResponse<Void>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<Void>> response =
                    testRestTemplate.exchange(
                            Uris.Like.CANCEL,
                            HttpMethod.DELETE,
                            new HttpEntity<>(null, headers),
                            responseType,
                            testProductId
                    );

            // then - 멱등성 보장으로 성공
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK)
            );
        }

        @Test
        @DisplayName("존재하지 않는 사용자로 좋아요 취소 시 404를 응답한다")
        void unlike_product_fail_when_user_not_found() {
            // given
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", "nonExistentUser");

            // when
            ParameterizedTypeReference<ApiResponse<Void>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<Void>> response =
                    testRestTemplate.exchange(
                            Uris.Like.CANCEL,
                            HttpMethod.DELETE,
                            new HttpEntity<>(null, headers),
                            responseType,
                            testProductId
                    );

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
            );
        }

        @Test
        @DisplayName("존재하지 않는 상품으로 좋아요 취소 시 404를 응답한다")
        void unlike_product_fail_when_product_not_found() {
            // given
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", testUsername);
            Long nonExistentProductId = 99999L;

            // when
            ParameterizedTypeReference<ApiResponse<Void>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<Void>> response =
                    testRestTemplate.exchange(
                            Uris.Like.CANCEL,
                            HttpMethod.DELETE,
                            new HttpEntity<>(null, headers),
                            responseType,
                            nonExistentProductId
                    );

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
            );
        }
    }
}

