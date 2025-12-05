package com.loopers.interfaces.api;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.common.vo.Price;
import com.loopers.domain.metrics.product.ProductMetrics;
import com.loopers.domain.product.Product;
import com.loopers.domain.supply.Supply;
import com.loopers.domain.supply.SupplyService;
import com.loopers.domain.supply.vo.Stock;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.metrics.product.ProductMetricsJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.interfaces.api.product.ProductV1Dto;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ProductV1ApiE2ETest {

    private final String ENDPOINT_PRODUCTS = "/api/v1/products";

    private final TestRestTemplate testRestTemplate;
    private final DatabaseCleanUp databaseCleanUp;
    private final BrandJpaRepository brandJpaRepository;
    private final ProductJpaRepository productJpaRepository;
    private final ProductMetricsJpaRepository productMetricsJpaRepository;
private final      SupplyService supplyService;

    @Autowired
    public ProductV1ApiE2ETest(
            TestRestTemplate testRestTemplate,
            DatabaseCleanUp databaseCleanUp,
            BrandJpaRepository brandJpaRepository,
            ProductJpaRepository productJpaRepository,
            ProductMetricsJpaRepository productMetricsJpaRepository,
            SupplyService supplyService
    ) {
        this.testRestTemplate = testRestTemplate;
        this.databaseCleanUp = databaseCleanUp;
        this.brandJpaRepository = brandJpaRepository;
        this.productJpaRepository = productJpaRepository;
        this.productMetricsJpaRepository = productMetricsJpaRepository;
        this.supplyService = supplyService;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    private Long brandId;
    private Long productId1;
    private Long productId2;

    @BeforeEach
    void setupProducts() {
        // Brand 등록
        Brand brand = Brand.create("Nike");
        Brand savedBrand = brandJpaRepository.save(brand);
        brandId = savedBrand.getId();

        // Product 등록
        Product product1 = createProduct("상품1", brandId, 10000);
        Product savedProduct1 = productJpaRepository.save(product1);
        productId1 = savedProduct1.getId();
        // ProductMetrics 등록
        ProductMetrics metrics1 = ProductMetrics.create(productId1, brandId, 0);
        productMetricsJpaRepository.save(metrics1);
        // Supply 등록
        Supply supply1 = Supply.create(productId1, new Stock(10));
        supplyService.save(supply1);

        Product product2 = createProduct("상품2", brandId, 20000);
        Product savedProduct2 = productJpaRepository.save(product2);
        productId2 = savedProduct2.getId();
        // ProductMetrics 등록
        ProductMetrics metrics2 = ProductMetrics.create(productId2, brandId, 0);
        productMetricsJpaRepository.save(metrics2);
        // Supply 등록
        Supply supply2 = Supply.create(productId2, new Stock(20));
        supplyService.save(supply2);
    }

    private Product createProduct(String name, Long brandId, int priceAmount) {
        return Product.create(name, brandId, new Price(priceAmount));
    }

    @DisplayName("GET /api/v1/products")
    @Nested
    class GetProductList {
        @DisplayName("비로그인 유저가 상품 목록 조회에 성공할 경우, 상품 목록을 응답으로 반환한다.")
        @Test
        void returnProductList_whenGetProductListSuccess() {
            // arrange

            // act
            ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductsPageResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<ProductV1Dto.ProductsPageResponse>> response = testRestTemplate.exchange(
                    ENDPOINT_PRODUCTS, HttpMethod.GET, new HttpEntity<>(null), responseType);

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data()).isNotNull(),
                    () -> assertThat(response.getBody().data().content()).isNotNull(),
                    () -> assertThat(response.getBody().data().size()).isGreaterThanOrEqualTo(2)
            );
        }

        @DisplayName("로그인한 유저가 상품 목록 조회에 성공할 경우, 상품 목록을 응답으로 반환한다.")
        @Test
        void returnProductList_whenLoggedInUser() {
            // arrange
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-USER-ID", "user123");

            // act
            ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductsPageResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<ProductV1Dto.ProductsPageResponse>> response = testRestTemplate.exchange(
                    ENDPOINT_PRODUCTS, HttpMethod.GET, new HttpEntity<>(null, headers), responseType);

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data()).isNotNull(),
                    () -> assertThat(response.getBody().data().content()).isNotNull(),
                    () -> assertThat(response.getBody().data().size()).isGreaterThanOrEqualTo(2)
            );
        }

        @DisplayName("페이지네이션 파라미터로 조회할 경우, 해당 페이지의 상품 목록을 반환한다.")
        @Test
        void returnProductList_whenWithPagination() {
            // arrange
            String url = ENDPOINT_PRODUCTS + "?page=0&size=10";

            // act
            ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductsPageResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<ProductV1Dto.ProductsPageResponse>> response = testRestTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(null), responseType);

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data()).isNotNull()
            );
        }

        @DisplayName("가격 오름차순 정렬로 조회할 경우, 가격이 낮은 순으로 상품 목록을 반환한다.")
        @Test
        void returnProductList_whenSortedByPriceAsc() {
            // arrange
            String url = ENDPOINT_PRODUCTS + "?sort=price_asc";

            // act
            ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductsPageResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<ProductV1Dto.ProductsPageResponse>> response = testRestTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(null), responseType);

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data()).isNotNull(),
                    // 가격 오름차순 정렬 검증
                    () -> {
                        var products = response.getBody().data().content();
                        for (int i = 0; i < products.size() - 1; i++) {
                            assertThat(products.get(i).price()).isLessThanOrEqualTo(products.get(i + 1).price());
                        }
                    }
            );
        }

        @DisplayName("좋아요 내림차순 정렬로 조회할 경우, 좋아요가 많은 순으로 상품 목록을 반환한다.")
        @Test
        void returnProductList_whenSortedByLikesDesc() {
            // arrange
            String url = ENDPOINT_PRODUCTS + "?sort=like_desc";

            // act
            ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductsPageResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<ProductV1Dto.ProductsPageResponse>> response = testRestTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(null), responseType);

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data()).isNotNull(),
                    // 좋아요 내림차순 정렬 검증
                    () -> {
                        var products = response.getBody().data().content();
                        for (int i = 0; i < products.size() - 1; i++) {
                            assertThat(products.get(i).likes()).isGreaterThanOrEqualTo(products.get(i + 1).likes());
                        }
                    }
            );
        }
    }

    @DisplayName("GET /api/v1/products/{productId}")
    @Nested
    class GetProductDetail {
        @DisplayName("존재하는 상품 ID로 조회할 경우, 상품 상세 정보를 응답으로 반환한다.")
        @Test
        void returnProductDetail_whenProductExists() {
            // arrange
            String url = ENDPOINT_PRODUCTS + "/" + productId1;

            // act
            ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<ProductV1Dto.ProductResponse>> response = testRestTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(null), responseType);

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data()).isNotNull(),
                    () -> assertThat(response.getBody().data().id()).isEqualTo(productId1),
                    () -> assertThat(response.getBody().data().name()).isEqualTo("상품1"),
                    () -> assertThat(response.getBody().data().price()).isEqualTo(10000)
            );
        }

        @DisplayName("존재하지 않는 상품 ID로 조회할 경우, `404 Not Found` 응답을 반환한다.")
        @Test
        void returnNotFound_whenProductIdDoesNotExist() {
            // arrange
            Long nonExistentProductId = 99999L;
            String url = ENDPOINT_PRODUCTS + "/" + nonExistentProductId;

            // act
            ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<ProductV1Dto.ProductResponse>> response = testRestTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(null), responseType);

            // assert
            assertThat(response.getStatusCode().value()).isEqualTo(404);
        }
    }
}
