package com.loopers.interfaces.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.interfaces.api.common.PageResponse;
import com.loopers.interfaces.api.product.ProductV1Dtos;
import com.loopers.support.Uris;
import com.loopers.utils.DatabaseCleanUp;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Product API E2E 테스트")
class ProductV1ApiE2ETest {

    private final TestRestTemplate testRestTemplate;
    private final DatabaseCleanUp databaseCleanUp;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private BrandService brandService;

    @Autowired
    private UserFacade userFacade;

    @Autowired
    public ProductV1ApiE2ETest(
            TestRestTemplate testRestTemplate,
            DatabaseCleanUp databaseCleanUp
    ) {
        this.testRestTemplate = testRestTemplate;
        this.databaseCleanUp = databaseCleanUp;
    }

    private Long testBrandId;
    private final List<Long> testProductIds = new ArrayList<>();
    private String testUsername;

    @BeforeEach
    void setUp() {
        // 테스트용 브랜드 생성
        BrandEntity brand = brandService.registerBrand(
                BrandTestFixture.createRequest("테스트브랜드", "E2E 테스트용 브랜드")
        );
        testBrandId = brand.getId();

        // 테스트용 상품 여러 개 생성
        for (int i = 1; i <= 5; i++) {
            ProductDomainCreateRequest productRequest = ProductTestFixture.createRequest(
                    testBrandId,
                    "테스트상품" + i,
                    "E2E 테스트용 상품 " + i,
                    new BigDecimal(String.valueOf(10000 * i)),
                    new BigDecimal(String.valueOf(7000 * i)),
                    100
            );
            ProductEntity product = productService.registerProduct(productRequest);
            testProductIds.add(product.getId());
        }

        // 테스트용 사용자 생성 (상품 상세 조회 시 좋아요 정보 테스트용)
        UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
        userFacade.registerUser(userCommand);
        testUsername = userCommand.username();
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
        testProductIds.clear();
        testBrandId = null;
        testUsername = null;
    }

    @Nested
    @DisplayName("상품 목록 조회 API")
    class GetProductsTest {

        @Test
        @DisplayName("상품 목록을 페이징하여 조회한다")
        void get_products_with_pagination_success() {
            // when
            ParameterizedTypeReference<ApiResponse<PageResponse<ProductV1Dtos.ProductListResponse>>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<PageResponse<ProductV1Dtos.ProductListResponse>>> response =
                    testRestTemplate.exchange(Uris.Product.GET_LIST + "?page=0&size=3",
                            HttpMethod.GET, null, responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().content()).hasSize(3),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().totalElements())
                            .isEqualTo(5),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().totalPages())
                            .isEqualTo(2),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().first()).isTrue(),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().last()).isFalse()
            );
        }

        @Test
        @DisplayName("두 번째 페이지를 조회하면 남은 상품들을 응답한다")
        void get_products_second_page_success() {
            // when
            ParameterizedTypeReference<ApiResponse<PageResponse<ProductV1Dtos.ProductListResponse>>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<PageResponse<ProductV1Dtos.ProductListResponse>>> response =
                    testRestTemplate.exchange(Uris.Product.GET_LIST + "?page=1&size=3",
                            HttpMethod.GET, null, responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().content()).hasSize(2),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().pageNumber()).isEqualTo(1),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().first()).isFalse(),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().last()).isTrue()
            );
        }

        @Test
        @DisplayName("기본 페이지 크기로 조회하면 20개씩 조회된다")
        void get_products_with_default_page_size_success() {
            // given - 21개의 상품 추가 생성 (총 26개)
            for (int i = 6; i <= 26; i++) {
                ProductDomainCreateRequest productRequest = ProductTestFixture.createRequest(
                        testBrandId,
                        "추가상품" + i,
                        "추가 상품 " + i,
                        new BigDecimal("5000"),
                        50
                );
                productService.registerProduct(productRequest);
            }

            // when
            ParameterizedTypeReference<ApiResponse<PageResponse<ProductV1Dtos.ProductListResponse>>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<PageResponse<ProductV1Dtos.ProductListResponse>>> response =
                    testRestTemplate.exchange(Uris.Product.GET_LIST,
                            HttpMethod.GET, null, responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().content()).hasSize(20),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().pageSize()).isEqualTo(20),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().totalElements())
                            .isEqualTo(26)
            );
        }

        @Test
        @DisplayName("상품이 없으면 빈 목록을 응답한다")
        void get_products_returns_empty_list_when_no_products() {
            // given - 모든 상품 삭제
            testProductIds.forEach(productId -> {
                ProductEntity product = productService.getProductDetail(productId);
                product.delete();
                productJpaRepository.save(product);

            });

            // when
            ParameterizedTypeReference<ApiResponse<PageResponse<ProductV1Dtos.ProductListResponse>>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<PageResponse<ProductV1Dtos.ProductListResponse>>> response =
                    testRestTemplate.exchange(Uris.Product.GET_LIST,
                            HttpMethod.GET, null, responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().content()).isEmpty(),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().totalElements())
                            .isEqualTo(0),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().empty()).isTrue()
            );
        }
    }

    @Nested
    @DisplayName("상품 상세 조회 API")
    class GetProductDetailTest {

        @Test
        @DisplayName("상품 ID로 상품 상세 정보를 조회한다")
        void get_product_detail_success() {
            // given
            Long productId = testProductIds.get(0);

            // when
            ParameterizedTypeReference<ApiResponse<ProductV1Dtos.ProductDetailResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<ProductV1Dtos.ProductDetailResponse>> response =
                    testRestTemplate.exchange(Uris.Product.GET_DETAIL,
                            HttpMethod.GET, null, responseType, productId);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().productId())
                            .isEqualTo(productId),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().name())
                            .isEqualTo("테스트상품1"),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().brand()).isNotNull(),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().brand().brandId())
                            .isEqualTo(testBrandId),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().isLiked())
                            .isFalse() // 비로그인 사용자는 좋아요 안 함
            );
        }

        @Test
        @DisplayName("로그인한 사용자는 좋아요 정보를 함께 조회한다")
        void get_product_detail_with_user_success() {
            // given
            Long productId = testProductIds.get(0);
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", testUsername);

            // when
            ParameterizedTypeReference<ApiResponse<ProductV1Dtos.ProductDetailResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<ProductV1Dtos.ProductDetailResponse>> response =
                    testRestTemplate.exchange(Uris.Product.GET_DETAIL,
                            HttpMethod.GET, new HttpEntity<>(null, headers), responseType, productId);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().productId())
                            .isEqualTo(productId),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().isLiked())
                            .isFalse() // 아직 좋아요 하지 않음
            );
        }

        @Test
        @DisplayName("상품 상세 정보에 가격 정보가 포함된다")
        void get_product_detail_includes_price_info() {
            // given
            Long productId = testProductIds.get(0);

            // when
            ParameterizedTypeReference<ApiResponse<ProductV1Dtos.ProductDetailResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<ProductV1Dtos.ProductDetailResponse>> response =
                    testRestTemplate.exchange(Uris.Product.GET_DETAIL,
                            HttpMethod.GET, null, responseType, productId);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().price()).isNotNull(),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().price().originPrice())
                            .isEqualByComparingTo(new BigDecimal("10000")),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().price().discountPrice())
                            .isNotNull()
            );
        }

        @Test
        @DisplayName("존재하지 않는 상품 ID로 조회하면 404 Not Found를 응답한다")
        void get_product_detail_fail_when_product_not_found() {
            // given
            Long nonExistentProductId = 99999L;

            // when
            ParameterizedTypeReference<ApiResponse<ProductV1Dtos.ProductDetailResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<ProductV1Dtos.ProductDetailResponse>> response =
                    testRestTemplate.exchange(Uris.Product.GET_DETAIL,
                            HttpMethod.GET, null, responseType, nonExistentProductId);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
            );
        }

        @Test
        @DisplayName("삭제된 상품 ID로 조회하면 404 Not Found를 응답한다")
        void get_product_detail_fail_when_product_deleted() {
            // given
            Long productId = testProductIds.get(0);
            ProductEntity product = productService.getProductDetail(productId);
            product.delete();
            productJpaRepository.save(product);

            // when
            ParameterizedTypeReference<ApiResponse<ProductV1Dtos.ProductDetailResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<ProductV1Dtos.ProductDetailResponse>> response =
                    testRestTemplate.exchange(Uris.Product.GET_DETAIL,
                            HttpMethod.GET, null, responseType, productId);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
            );
        }

        @Test
        @DisplayName("숫자가 아닌 상품 ID로 조회하면 400 Bad Request를 응답한다")
        void get_product_detail_fail_when_invalid_product_id() {
            // when
            ParameterizedTypeReference<ApiResponse<ProductV1Dtos.ProductDetailResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<ProductV1Dtos.ProductDetailResponse>> response =
                    testRestTemplate.exchange("/api/v1/products/invalid",
                            HttpMethod.GET, null, responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
            );
        }
    }
}

