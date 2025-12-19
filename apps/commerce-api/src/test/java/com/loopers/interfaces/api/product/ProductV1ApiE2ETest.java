package com.loopers.interfaces.api.product;

import com.loopers.application.members.MemberFacade;
import com.loopers.application.members.MemberInfo;
import com.loopers.domain.members.enums.Gender;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.repository.BrandRepository;
import com.loopers.domain.common.vo.Money;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.vo.Stock;
import com.loopers.domain.product.repository.ProductRepository;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.utils.DatabaseCleanUp;
import com.loopers.utils.RestPage;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductV1ApiE2ETest {

    private final TestRestTemplate testRestTemplate;
    private final MemberFacade memberFacade;
    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final DatabaseCleanUp databaseCleanUp;

    @Autowired
    public ProductV1ApiE2ETest(
            TestRestTemplate testRestTemplate,
            MemberFacade memberFacade,
            BrandRepository brandRepository,
            ProductRepository productRepository,
            DatabaseCleanUp databaseCleanUp
    ) {
        this.testRestTemplate = testRestTemplate;
        this.memberFacade = memberFacade;
        this.brandRepository = brandRepository;
        this.productRepository = productRepository;
        this.databaseCleanUp = databaseCleanUp;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    private record TestContext(Long memberId, Long productId) {}

    private TestContext setupProductAndMember() {
        MemberInfo member = memberFacade.registerMember("test123", "test@example.com", "password", "1990-01-01", Gender.MALE);

        Brand brand = brandRepository.save(new Brand("TestBrand", "Test Brand Description"));

        Product product = new Product(
                brand.getId(),
                "Test Product",
                "Test Description",
                Money.of(BigDecimal.valueOf(10000)),
                Stock.of(100)
        );
        Long productId = productRepository.save(product).getId();
        return new TestContext(member.id(), productId);
    }

    private HttpHeaders headersOf(Long memberId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-USER-ID", String.valueOf(memberId));
        return headers;
    }

    @DisplayName("상품 목록 조회 (GET /api/v1/products)")
    @Nested
    class GetProducts {

        @DisplayName("검색 키워드 없이 조회 시 모든 상품을 반환한다")
        @Test
        void shouldReturnAllProducts_whenNoKeyword() {
            TestContext context = setupProductAndMember();

            HttpHeaders headers = headersOf(context.memberId());

            ParameterizedTypeReference<ApiResponse<RestPage<ProductV1Dto.ProductSummaryResponse>>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<RestPage<ProductV1Dto.ProductSummaryResponse>>> response =
                    testRestTemplate.exchange("/api/v1/products", HttpMethod.GET,
                            new HttpEntity<>(null, headers), responseType);

            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data()).isNotNull(),
                    () -> assertThat(response.getBody().data().getTotalElements()).isGreaterThan(0)
            );
        }

        @DisplayName("검색 키워드로 조회 시 해당하는 상품을 반환한다")
        @Test
        void shouldReturnMatchingProducts_whenKeywordProvided() {
            TestContext context = setupProductAndMember();

            HttpHeaders headers = headersOf(context.memberId());

            ParameterizedTypeReference<ApiResponse<RestPage<ProductV1Dto.ProductSummaryResponse>>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<RestPage<ProductV1Dto.ProductSummaryResponse>>> response =
                    testRestTemplate.exchange("/api/v1/products?keyword=Test", HttpMethod.GET,
                            new HttpEntity<>(null, headers), responseType);

            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data()).isNotNull(),
                    () -> assertThat(response.getBody().data().getTotalElements()).isGreaterThan(0)
            );
        }

        @DisplayName("brandId로 조회 시 해당 브랜드의 상품만 반환한다")
        @Test
        void shouldReturnProductsOfBrand_whenBrandIdProvided() {
            // given
            Long memberId = memberFacade.registerMember("test123", "test@example.com", "password", "1990-01-01", Gender.MALE).id();

            Brand brand1 = brandRepository.save(new Brand("Nike", "Nike Brand"));
            Brand brand2 = brandRepository.save(new Brand("Adidas", "Adidas Brand"));

            productRepository.save(new Product(
                    brand1.getId(),
                    "Nike Shoes",
                    "Running shoes",
                    Money.of(BigDecimal.valueOf(100000)),
                    Stock.of(50)
            ));

            productRepository.save(new Product(
                    brand1.getId(),
                    "Nike Shirt",
                    "Sports shirt",
                    Money.of(BigDecimal.valueOf(50000)),
                    Stock.of(100)
            ));

            productRepository.save(new Product(
                    brand2.getId(),
                    "Adidas Shoes",
                    "Football shoes",
                    Money.of(BigDecimal.valueOf(120000)),
                    Stock.of(30)
            ));

            HttpHeaders headers = headersOf(memberId);

            // when
            ParameterizedTypeReference<ApiResponse<RestPage<ProductV1Dto.ProductSummaryResponse>>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<RestPage<ProductV1Dto.ProductSummaryResponse>>> response =
                    testRestTemplate.exchange("/api/v1/products?brandId=" + brand1.getId(), HttpMethod.GET,
                            new HttpEntity<>(null, headers), responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data()).isNotNull(),
                    () -> assertThat(response.getBody().data().getTotalElements()).isEqualTo(2),
                    () -> assertThat(response.getBody().data().getContent()).allMatch(
                            product -> product.brandName().equals("Nike")
                    )
            );
        }

        @DisplayName("brandId와 keyword를 함께 사용하여 조회 시 두 조건 모두 만족하는 상품만 반환한다")
        @Test
        void shouldReturnMatchingProducts_whenBrandIdAndKeywordProvided() {
            // given
            Long memberId = memberFacade.registerMember("test123", "test@example.com", "password", "1990-01-01", Gender.MALE).id();

            Brand brand1 = brandRepository.save(new Brand("Nike", "Nike Brand"));
            Brand brand2 = brandRepository.save(new Brand("Adidas", "Adidas Brand"));

            productRepository.save(new Product(
                    brand1.getId(),
                    "Nike Shoes",
                    "Running shoes",
                    Money.of(BigDecimal.valueOf(100000)),
                    Stock.of(50)
            ));

            productRepository.save(new Product(
                    brand1.getId(),
                    "Nike Shirt",
                    "Sports shirt",
                    Money.of(BigDecimal.valueOf(50000)),
                    Stock.of(100)
            ));

            productRepository.save(new Product(
                    brand2.getId(),
                    "Adidas Shoes",
                    "Football shoes",
                    Money.of(BigDecimal.valueOf(120000)),
                    Stock.of(30)
            ));

            HttpHeaders headers = headersOf(memberId);

            // when
            ParameterizedTypeReference<ApiResponse<RestPage<ProductV1Dto.ProductSummaryResponse>>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<RestPage<ProductV1Dto.ProductSummaryResponse>>> response =
                    testRestTemplate.exchange(
                            "/api/v1/products?brandId=" + brand1.getId() + "&keyword=Shoes",
                            HttpMethod.GET,
                            new HttpEntity<>(null, headers),
                            responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data()).isNotNull(),
                    () -> assertThat(response.getBody().data().getTotalElements()).isEqualTo(1),
                    () -> assertThat(response.getBody().data().getContent().get(0).name()).isEqualTo("Nike Shoes"),
                    () -> assertThat(response.getBody().data().getContent().get(0).brandName()).isEqualTo("Nike")
            );
        }
    }

    @DisplayName("상품 상세 조회 (GET /api/v1/products/{productId})")
    @Nested
    class GetProductDetail {

        @DisplayName("유효한 상품 ID로 조회 시 상품 상세 정보를 반환한다")
        @Test
        void shouldReturnProductDetail_whenValidProductId() {
            TestContext context = setupProductAndMember();
            Long productId = context.productId();

            HttpHeaders headers = headersOf(context.memberId());

            ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductDetailResponse>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<ProductV1Dto.ProductDetailResponse>> response =
                    testRestTemplate.exchange("/api/v1/products/" + productId, HttpMethod.GET,
                            new HttpEntity<>(null, headers), responseType);

            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data()).isNotNull(),
                    () -> assertThat(response.getBody().data().id()).isEqualTo(productId),
                    () -> assertThat(response.getBody().data().name()).isEqualTo("Test Product")
            );
        }

        @DisplayName("존재하지 않는 상품 ID로 조회 시 404를 반환한다")
        @Test
        void shouldReturn404_whenProductNotFound() {
            Long memberId = memberFacade.registerMember("test123", "test@example.com", "password", "1990-01-01", Gender.MALE).id();

            HttpHeaders headers = headersOf(memberId);

            ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductDetailResponse>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<ProductV1Dto.ProductDetailResponse>> response =
                    testRestTemplate.exchange("/api/v1/products/999", HttpMethod.GET,
                            new HttpEntity<>(null, headers), responseType);

            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
            );
        }
    }

    @DisplayName("인기 상품 TOP100 조회 (GET /api/v1/products/popular)")
    @Nested
    class GetPopularProducts {

        @DisplayName("좋아요 수 기준으로 상위 100개 상품을 반환한다")
        @Test
        void shouldReturnTop100Products_orderedByLikesDesc() {
            // given
            Long memberId = memberFacade.registerMember("test123", "test@example.com", "password", "1990-01-01", Gender.MALE).id();
            Brand brand = brandRepository.save(new Brand("TestBrand", "Test Brand Description"));

            // 150개의 상품 생성 (좋아요 수는 랜덤)
            for (int i = 1; i <= 150; i++) {
                Product product = new Product(
                        brand.getId(),
                        "Product " + i,
                        "Description " + i,
                        Money.of(BigDecimal.valueOf(10000 + i * 100)),
                        Stock.of(100)
                );
                // 좋아요 수 설정 (150번째가 가장 많고, 1번째가 가장 적음)
                product.setLikeCount(i);
                productRepository.save(product);
            }

            HttpHeaders headers = headersOf(memberId);

            // when
            ParameterizedTypeReference<ApiResponse<java.util.List<ProductV1Dto.ProductSummaryResponse>>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<java.util.List<ProductV1Dto.ProductSummaryResponse>>> response =
                    testRestTemplate.exchange("/api/v1/products/popular", HttpMethod.GET,
                            new HttpEntity<>(null, headers), responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data()).isNotNull(),
                    () -> assertThat(response.getBody().data()).hasSize(100),
                    // 첫 번째 상품은 좋아요 150개
                    () -> assertThat(response.getBody().data().get(0).likeCount()).isEqualTo(150),
                    () -> assertThat(response.getBody().data().get(0).name()).isEqualTo("Product 150"),
                    // 마지막(100번째) 상품은 좋아요 51개
                    () -> assertThat(response.getBody().data().get(99).likeCount()).isEqualTo(51),
                    () -> assertThat(response.getBody().data().get(99).name()).isEqualTo("Product 51"),
                    // 좋아요 수가 내림차순으로 정렬되어 있는지 확인
                    () -> {
                        java.util.List<ProductV1Dto.ProductSummaryResponse> products = response.getBody().data();
                        for (int i = 0; i < products.size() - 1; i++) {
                            assertThat(products.get(i).likeCount())
                                    .isGreaterThanOrEqualTo(products.get(i + 1).likeCount());
                        }
                    }
            );
        }

        @DisplayName("상품이 100개 미만일 때 전체 상품을 반환한다")
        @Test
        void shouldReturnAllProducts_whenLessThan100Products() {
            // given
            Long memberId = memberFacade.registerMember("test123", "test@example.com", "password", "1990-01-01", Gender.MALE).id();
            Brand brand = brandRepository.save(new Brand("TestBrand", "Test Brand Description"));

            // 50개의 상품만 생성
            for (int i = 1; i <= 50; i++) {
                Product product = new Product(
                        brand.getId(),
                        "Product " + i,
                        "Description " + i,
                        Money.of(BigDecimal.valueOf(10000)),
                        Stock.of(100)
                );
                product.setLikeCount(i);
                productRepository.save(product);
            }

            HttpHeaders headers = headersOf(memberId);

            // when
            ParameterizedTypeReference<ApiResponse<java.util.List<ProductV1Dto.ProductSummaryResponse>>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<java.util.List<ProductV1Dto.ProductSummaryResponse>>> response =
                    testRestTemplate.exchange("/api/v1/products/popular", HttpMethod.GET,
                            new HttpEntity<>(null, headers), responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data()).isNotNull(),
                    () -> assertThat(response.getBody().data()).hasSize(50),
                    () -> assertThat(response.getBody().data().get(0).likeCount()).isEqualTo(50),
                    () -> assertThat(response.getBody().data().get(49).likeCount()).isEqualTo(1)
            );
        }

        @DisplayName("로그인하지 않은 사용자도 인기 상품을 조회할 수 있다")
        @Test
        void shouldReturnPopularProducts_withoutLogin() {
            // given
            Brand brand = brandRepository.save(new Brand("TestBrand", "Test Brand Description"));

            for (int i = 1; i <= 10; i++) {
                Product product = new Product(
                        brand.getId(),
                        "Product " + i,
                        "Description " + i,
                        Money.of(BigDecimal.valueOf(10000)),
                        Stock.of(100)
                );
                product.setLikeCount(i);
                productRepository.save(product);
            }

            // when - X-USER-ID 헤더 없이 요청
            ParameterizedTypeReference<ApiResponse<java.util.List<ProductV1Dto.ProductSummaryResponse>>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<java.util.List<ProductV1Dto.ProductSummaryResponse>>> response =
                    testRestTemplate.exchange("/api/v1/products/popular", HttpMethod.GET,
                            new HttpEntity<>(null, null), responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data()).isNotNull(),
                    () -> assertThat(response.getBody().data()).hasSize(10),
                    // 로그인하지 않았으므로 모든 상품의 isLikedByMember는 false
                    () -> assertThat(response.getBody().data())
                            .allMatch(product -> !product.isLikedByMember())
            );
        }
    }

    @DisplayName("브랜드별 인기 상품 TOP N 조회 (GET /api/v1/brands/{brandId}/products/popular)")
    @Nested
    class GetBrandPopularProducts {

        @DisplayName("특정 브랜드의 인기 상품 TOP N을 반환한다")
        @Test
        void shouldReturnTopNProductsForBrand_orderedByLikesDesc() {
            // given
            Long memberId = memberFacade.registerMember("test123", "test@example.com", "password", "1990-01-01", Gender.MALE).id();

            Brand brand1 = brandRepository.save(new Brand("Nike", "Nike Brand"));
            Brand brand2 = brandRepository.save(new Brand("Adidas", "Adidas Brand"));

            // Nike 브랜드 상품 10개 생성 (좋아요 수 1~10)
            for (int i = 1; i <= 10; i++) {
                Product product = new Product(
                        brand1.getId(),
                        "Nike Product " + i,
                        "Description " + i,
                        Money.of(BigDecimal.valueOf(10000 + i * 100)),
                        Stock.of(100)
                );
                product.setLikeCount(i);
                productRepository.save(product);
            }

            // Adidas 브랜드 상품 5개 생성
            for (int i = 1; i <= 5; i++) {
                Product product = new Product(
                        brand2.getId(),
                        "Adidas Product " + i,
                        "Description " + i,
                        Money.of(BigDecimal.valueOf(10000)),
                        Stock.of(100)
                );
                product.setLikeCount(i * 10);
                productRepository.save(product);
            }

            HttpHeaders headers = headersOf(memberId);

            // when - Nike 브랜드의 TOP 5 조회
            ParameterizedTypeReference<ApiResponse<java.util.List<ProductV1Dto.ProductSummaryResponse>>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<java.util.List<ProductV1Dto.ProductSummaryResponse>>> response =
                    testRestTemplate.exchange(
                            "/api/v1/brands/" + brand1.getId() + "/products/popular?limit=5",
                            HttpMethod.GET,
                            new HttpEntity<>(null, headers),
                            responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data()).isNotNull(),
                    () -> assertThat(response.getBody().data()).hasSize(5),
                    // 첫 번째 상품은 좋아요 10개
                    () -> assertThat(response.getBody().data().get(0).likeCount()).isEqualTo(10),
                    () -> assertThat(response.getBody().data().get(0).name()).isEqualTo("Nike Product 10"),
                    () -> assertThat(response.getBody().data().get(0).brandName()).isEqualTo("Nike"),
                    // 마지막(5번째) 상품은 좋아요 6개
                    () -> assertThat(response.getBody().data().get(4).likeCount()).isEqualTo(6),
                    () -> assertThat(response.getBody().data().get(4).name()).isEqualTo("Nike Product 6"),
                    // 모든 상품이 Nike 브랜드
                    () -> assertThat(response.getBody().data())
                            .allMatch(product -> product.brandName().equals("Nike")),
                    // 좋아요 수가 내림차순으로 정렬되어 있는지 확인
                    () -> {
                        java.util.List<ProductV1Dto.ProductSummaryResponse> products = response.getBody().data();
                        for (int i = 0; i < products.size() - 1; i++) {
                            assertThat(products.get(i).likeCount())
                                    .isGreaterThanOrEqualTo(products.get(i + 1).likeCount());
                        }
                    }
            );
        }

        @DisplayName("limit 파라미터 없이 요청하면 기본값 10개를 반환한다")
        @Test
        void shouldReturnDefaultLimit10_whenLimitNotProvided() {
            // given
            Long memberId = memberFacade.registerMember("test123", "test@example.com", "password", "1990-01-01", Gender.MALE).id();
            Brand brand = brandRepository.save(new Brand("Nike", "Nike Brand"));

            // 15개의 상품 생성
            for (int i = 1; i <= 15; i++) {
                Product product = new Product(
                        brand.getId(),
                        "Product " + i,
                        "Description",
                        Money.of(BigDecimal.valueOf(10000)),
                        Stock.of(100)
                );
                product.setLikeCount(i);
                productRepository.save(product);
            }

            HttpHeaders headers = headersOf(memberId);

            // when
            ParameterizedTypeReference<ApiResponse<java.util.List<ProductV1Dto.ProductSummaryResponse>>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<java.util.List<ProductV1Dto.ProductSummaryResponse>>> response =
                    testRestTemplate.exchange(
                            "/api/v1/brands/" + brand.getId() + "/products/popular",
                            HttpMethod.GET,
                            new HttpEntity<>(null, headers),
                            responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data()).hasSize(10)
            );
        }

        @DisplayName("해당 브랜드의 상품이 limit보다 적으면 전체를 반환한다")
        @Test
        void shouldReturnAllProducts_whenLessThanLimit() {
            // given
            Long memberId = memberFacade.registerMember("test123", "test@example.com", "password", "1990-01-01", Gender.MALE).id();
            Brand brand = brandRepository.save(new Brand("Nike", "Nike Brand"));

            // 3개의 상품만 생성
            for (int i = 1; i <= 3; i++) {
                Product product = new Product(
                        brand.getId(),
                        "Product " + i,
                        "Description",
                        Money.of(BigDecimal.valueOf(10000)),
                        Stock.of(100)
                );
                product.setLikeCount(i);
                productRepository.save(product);
            }

            HttpHeaders headers = headersOf(memberId);

            // when - limit=10으로 요청했지만 3개만 반환되어야 함
            ParameterizedTypeReference<ApiResponse<java.util.List<ProductV1Dto.ProductSummaryResponse>>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<java.util.List<ProductV1Dto.ProductSummaryResponse>>> response =
                    testRestTemplate.exchange(
                            "/api/v1/brands/" + brand.getId() + "/products/popular?limit=10",
                            HttpMethod.GET,
                            new HttpEntity<>(null, headers),
                            responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data()).hasSize(3),
                    () -> assertThat(response.getBody().data().get(0).likeCount()).isEqualTo(3),
                    () -> assertThat(response.getBody().data().get(2).likeCount()).isEqualTo(1)
            );
        }

        @DisplayName("해당 브랜드의 상품이 없으면 빈 리스트를 반환한다")
        @Test
        void shouldReturnEmptyList_whenNoProducts() {
            // given
            Long memberId = memberFacade.registerMember("test123", "test@example.com", "password", "1990-01-01", Gender.MALE).id();
            Brand brand = brandRepository.save(new Brand("Nike", "Nike Brand"));

            HttpHeaders headers = headersOf(memberId);

            // when
            ParameterizedTypeReference<ApiResponse<java.util.List<ProductV1Dto.ProductSummaryResponse>>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<java.util.List<ProductV1Dto.ProductSummaryResponse>>> response =
                    testRestTemplate.exchange(
                            "/api/v1/brands/" + brand.getId() + "/products/popular",
                            HttpMethod.GET,
                            new HttpEntity<>(null, headers),
                            responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data()).isEmpty()
            );
        }

        @DisplayName("존재하지 않는 브랜드 ID로 조회 시 404를 반환한다")
        @Test
        void shouldReturn404_whenBrandNotFound() {
            // given
            Long memberId = memberFacade.registerMember("test123", "test@example.com", "password", "1990-01-01", Gender.MALE).id();

            HttpHeaders headers = headersOf(memberId);

            // when
            ParameterizedTypeReference<ApiResponse<java.util.List<ProductV1Dto.ProductSummaryResponse>>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<java.util.List<ProductV1Dto.ProductSummaryResponse>>> response =
                    testRestTemplate.exchange(
                            "/api/v1/brands/99999/products/popular",
                            HttpMethod.GET,
                            new HttpEntity<>(null, headers),
                            responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
            );
        }

        @DisplayName("로그인하지 않은 사용자도 브랜드별 인기 상품을 조회할 수 있다")
        @Test
        void shouldReturnBrandPopularProducts_withoutLogin() {
            // given
            Brand brand = brandRepository.save(new Brand("Nike", "Nike Brand"));

            for (int i = 1; i <= 5; i++) {
                Product product = new Product(
                        brand.getId(),
                        "Product " + i,
                        "Description",
                        Money.of(BigDecimal.valueOf(10000)),
                        Stock.of(100)
                );
                product.setLikeCount(i);
                productRepository.save(product);
            }

            // when - X-USER-ID 헤더 없이 요청
            ParameterizedTypeReference<ApiResponse<java.util.List<ProductV1Dto.ProductSummaryResponse>>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<java.util.List<ProductV1Dto.ProductSummaryResponse>>> response =
                    testRestTemplate.exchange(
                            "/api/v1/brands/" + brand.getId() + "/products/popular?limit=5",
                            HttpMethod.GET,
                            new HttpEntity<>(null, null),
                            responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data()).hasSize(5),
                    // 로그인하지 않았으므로 모든 상품의 isLikedByMember는 false
                    () -> assertThat(response.getBody().data())
                            .allMatch(product -> !product.isLikedByMember())
            );
        }
    }
}
