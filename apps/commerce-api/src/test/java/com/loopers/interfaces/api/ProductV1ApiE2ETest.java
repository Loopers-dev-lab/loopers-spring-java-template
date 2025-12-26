package com.loopers.interfaces.api;

import com.loopers.application.like.LikeFacade;
import com.loopers.application.product.ProductDetailInfo;
import com.loopers.application.product.ProductFacade;
import com.loopers.cache.CacheKeyService;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.common.vo.Money;
import com.loopers.domain.product.Product;
import com.loopers.domain.user.User;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.infrastructure.product.ProductLikeCountJpaRepository;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.interfaces.api.product.ProductV1Dto;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

import static com.loopers.support.fixture.UserFixtures.createValidUsers;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductV1ApiE2ETest {

    private static final String ENDPOINT_GET = "/api/v1/products";
    private static final Function<Long, String> ENDPOINT_GET_ID = id -> "/api/v1/products/" + id;

    private final TestRestTemplate testRestTemplate;
    private final UserJpaRepository userJpaRepository;
    private final BrandJpaRepository brandJpaRepository;
    private final ProductJpaRepository productJpaRepository;
    private final ProductLikeCountJpaRepository productLikeCountJpaRepository;

    private final LikeFacade likeFacade;
    private final ProductFacade productFacade;
    private final DatabaseCleanUp databaseCleanUp;

    private Brand brandA, brandB;
    private List<Product> products;
    private List<User> users;

    private RedisTemplate<String, String> redisTemplate;
    private CacheKeyService cacheKeyService;

    @Autowired
    public ProductV1ApiE2ETest(
            TestRestTemplate testRestTemplate,
            BrandJpaRepository brandJpaRepository,
            ProductJpaRepository productJpaRepository,
            UserJpaRepository userJpaRepository,
            ProductLikeCountJpaRepository productLikeCountJpaRepository,
            LikeFacade likeFacade,
            ProductFacade productFacade,
            CacheKeyService cacheKeyService,
            RedisTemplate<String, String> redisTemplate,
            DatabaseCleanUp databaseCleanUp
    ) {
        this.testRestTemplate = testRestTemplate;
        this.brandJpaRepository = brandJpaRepository;
        this.productJpaRepository = productJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.productLikeCountJpaRepository = productLikeCountJpaRepository;
        this.likeFacade = likeFacade;
        this.productFacade = productFacade;
        this.cacheKeyService = cacheKeyService;
        this.redisTemplate = redisTemplate;
        this.databaseCleanUp = databaseCleanUp;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @BeforeEach
    void setUp() {
        // 사용자 생성
        users = createValidUsers(40);
        IntStream.range(0, 40).forEach(i -> {
            userJpaRepository.save(users.get(i));
        });

        // 브랜드 생성
        brandA = brandJpaRepository.save(Brand.create("브랜드A", "브랜드A 설명"));
        brandB = brandJpaRepository.save(Brand.create("브랜드B", "브랜드B 설명"));

        // 상품 생성
        products = new ArrayList<>();
        IntStream.range(0, 40).forEach(i -> {
            Brand brand = i%2 == 0 ? brandA : brandB;
            Product product = Product.create(brand.getId(), "상품"+(i+1), Money.of(1000 * (i+1)));
            products.add(productJpaRepository.save(product));
        });

        // 좋아요 + 좋아요 카운트 생성
        // 상품1 40번, 상품2 39번, 상품3 38번 ...
        IntStream.range(0, 40).forEach(i -> {
            IntStream.range(i, 40).forEach(j -> {
                likeFacade.addLike(users.get(j).getId(), products.get(i).getId());
            });
        });
    }

    @DisplayName("GET /api/v1/products")
    @Nested
    class GetList {
        @DisplayName("요청 시 상품의 총 좋아요 수가 포함된 페이징된 상품 목록을 반환한다.")
        @Test
        void succeeds_whenRequestingPagedProductsWithLikeCounts() {
            // arrange

            // act
            ParameterizedTypeReference<ApiResponse<ProductV1Dto.PageResponse<ProductV1Dto.ProductResponse>>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<ProductV1Dto.PageResponse<ProductV1Dto.ProductResponse>>> response =
                    testRestTemplate.exchange(ENDPOINT_GET, HttpMethod.GET, new HttpEntity<>(null), responseType);

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody().data()).isNotNull(),
                    () -> assertThat(response.getBody().data().size()).isEqualTo(20)
            );
        }

        @DisplayName("브랜드id로 필터링된 상품 목록을 반환한다.")
        @Test
        void succeeds_whenFilteringProductsByBrandId() {
            // arrange
            String param = "?brandId=" + brandA.getId();

            // act
            ParameterizedTypeReference<ApiResponse<ProductV1Dto.PageResponse<ProductV1Dto.ProductResponse>>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<ProductV1Dto.PageResponse<ProductV1Dto.ProductResponse>>> response =
                    testRestTemplate.exchange(ENDPOINT_GET+param, HttpMethod.GET, new HttpEntity<>(null), responseType);

            // assert
            ProductV1Dto.PageResponse<ProductV1Dto.ProductResponse> pageResponse = response.getBody().data();
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(pageResponse.size()).isEqualTo(20),
                    () -> assertThat(pageResponse.content().stream()
                                .filter(p -> p.brandId().equals(brandA.getId())).count()
                            ).isEqualTo(20)
            );

        }

        @DisplayName("가격 오름차순으로 정렬된 상품 목록을 반환한다.")
        @Test
        void succeeds_whenSortingProductsByPriceAsc() {
            // arrange
            String param = "?sort=price_asc&size=40";

            // act
            ParameterizedTypeReference<ApiResponse<ProductV1Dto.PageResponse<ProductV1Dto.ProductResponse>>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<ProductV1Dto.PageResponse<ProductV1Dto.ProductResponse>>> response =
                    testRestTemplate.exchange(ENDPOINT_GET+param, HttpMethod.GET, new HttpEntity<>(null), responseType);

            // assert
            assertTrue(response.getStatusCode().is2xxSuccessful());
            assertThat(response.getBody().data()).isNotNull();

            List<Long> expectedSortedIds = products.stream()
                    .sorted(Comparator.comparing(p -> p.getPrice().getAmount()))
                    .map(Product::getId)
                    .toList();

            List<Long> resultSortedIds = response.getBody().data().content().stream()
                    .map(ProductV1Dto.ProductResponse::productId)
                    .toList();

            assertThat(resultSortedIds).containsExactlyElementsOf(expectedSortedIds);
        }

        @DisplayName("좋아요 내림차순으로 정렬된 상품 목록을 반환한다.")
        @Test
        void succeeds_whenSortingProductsByLikesDesc() {
            // arrange
            String param = "?sort=likes_desc&size=40";
            List<Long> expectedSortedIds = products.stream()
                    .map(p -> productFacade.getProductDetail(p.getId()))
                    .sorted(Comparator.comparing(ProductDetailInfo::likeCount).reversed())
                    .map(ProductDetailInfo::productId)
                    .toList();

            // act
            ParameterizedTypeReference<ApiResponse<ProductV1Dto.PageResponse<ProductV1Dto.ProductResponse>>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<ProductV1Dto.PageResponse<ProductV1Dto.ProductResponse>>> response =
                    testRestTemplate.exchange(ENDPOINT_GET+param, HttpMethod.GET, new HttpEntity<>(null), responseType);

            // assert
            assertTrue(response.getStatusCode().is2xxSuccessful());
            assertThat(response.getBody().data()).isNotNull();

            List<Long> resultSortedIds = response.getBody().data().content().stream()
                    .map(ProductV1Dto.ProductResponse::productId)
                    .toList();

            assertThat(expectedSortedIds).containsExactlyElementsOf(resultSortedIds);
        }

        @DisplayName("최신순으로 정렬된 상품 목록을 반환한다.")
        @Test
        void succeeds_whenSortingProductsByLatest() {
            // arrange
            String param = "?sort=latest&size=40";

            // act
            ParameterizedTypeReference<ApiResponse<ProductV1Dto.PageResponse<ProductV1Dto.ProductResponse>>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<ProductV1Dto.PageResponse<ProductV1Dto.ProductResponse>>> response =
                    testRestTemplate.exchange(ENDPOINT_GET+param, HttpMethod.GET, new HttpEntity<>(null), responseType);

            // assert
            assertTrue(response.getStatusCode().is2xxSuccessful());
            assertThat(response.getBody().data()).isNotNull();

            List<Long> expectedSortedIds = products.stream()
                    .sorted(Comparator.comparing(Product::getCreatedAt).reversed())
                    .map(Product::getId)
                    .toList();

            List<Long> resultSortedIds = response.getBody().data().content().stream()
                    .map(ProductV1Dto.ProductResponse::productId)
                    .toList();

            assertThat(expectedSortedIds).containsExactlyElementsOf(resultSortedIds);
        }

        @DisplayName("목록 조회 시 캐시가 적용되어 두 번째 호출 시 첫 호출과 동일한 데이터를 반환한다.")
        @Test
        void succeeds_whenSecondCallHitsCache() {
            // arrange;
            String url = ENDPOINT_GET + "?brandId=" + brandA.getId();

            String cacheKey = cacheKeyService.productListKey(brandA.getId(), PageRequest.of(0, 20), null);

            // act (Cache Miss)
            ResponseEntity<ApiResponse<ProductV1Dto.PageResponse<ProductV1Dto.ProductResponse>>> firstResponse =
                    testRestTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});

            // 첫 호출 성공 및 캐시 저장 확인
            assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(redisTemplate.opsForValue().get(cacheKey)).isNotNull();

            // act (Cache Hit)
            ResponseEntity<ApiResponse<ProductV1Dto.PageResponse<ProductV1Dto.ProductResponse>>> secondResponse =
                    testRestTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});

            // assert: 두 응답의 내용이 완전히 동일한지 비교
            assertAll(
                () -> assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(secondResponse.getBody().data()).isEqualTo(firstResponse.getBody().data())
            );
        }
    }

    @DisplayName("GET /api/v1/products/{productId}")
    @Nested
    class GetListDetail {
        @DisplayName("유효한 productId로 조회 시 상품 상세 정보를 반환한다.")
        @Test
        void succeeds_whenProductExists() {
            // arrange
            Product product = products.stream()
                    .filter(p -> p.getId() == 1L)
                    .findFirst().get();

            int expectedLikeCount = productLikeCountJpaRepository.findById(product.getId()).get().getLikeCount();

            // act
            ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductDetailResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<ProductV1Dto.ProductDetailResponse>> response =
                    testRestTemplate.exchange(ENDPOINT_GET_ID.apply(1L), HttpMethod.GET, new HttpEntity<>(null), responseType);

            // assert
            ProductV1Dto.ProductDetailResponse productDetailResponse = response.getBody().data();
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(productDetailResponse).isNotNull(),
                    () -> assertThat(productDetailResponse.productId()).isEqualTo(product.getId()),
                    () -> assertThat(productDetailResponse.productName()).isEqualTo(product.getName()),
                    () -> assertThat(productDetailResponse.brandId()).isEqualTo(product.getBrandId()),
                    () -> assertThat(productDetailResponse.price()).isEqualByComparingTo(product.getPrice().getAmount()),
                    () -> assertThat(productDetailResponse.likeCount()).isEqualTo(expectedLikeCount)
            );
        }

        @DisplayName("존재하지 않는 productId로 조회 시 404 Not Found 응답을 반환한다.")
        @Test
        void fails_whenProductNotFound() {
            // arrange
            Long invalidId = -1L;

            // act
            ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductDetailResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<ProductV1Dto.ProductDetailResponse>> response =
                    testRestTemplate.exchange(ENDPOINT_GET_ID.apply(invalidId), HttpMethod.GET, new HttpEntity<>(null), responseType);

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
            );
        }

        @DisplayName("캐시가 적용되어 두 번째 호출 시 첫 호출과 동일한 데이터를 반환한다.")
        @Test
        void succeeds_whenSecondCallHitsCache() {
            // arrange
            Long productId = 1L;
            String url = ENDPOINT_GET_ID.apply(productId);
            String cacheKey = cacheKeyService.productDetailKey(productId);

            // act (Cache Miss)
            ResponseEntity<ApiResponse<ProductV1Dto.ProductDetailResponse>> firstResponse =
                    testRestTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});

            // 첫 호출이 성공했고, 캐시에 데이터가 저장되었는지 확인
            assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(redisTemplate.opsForValue().get(cacheKey)).isNotNull();
            
            // act (Cache Hit)
            ResponseEntity<ApiResponse<ProductV1Dto.ProductDetailResponse>> secondResponse =
                    testRestTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});

            // assert: 두 응답의 내용이 완전히 동일한지 비교
            assertAll(
                () -> assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(secondResponse.getBody().data()).isEqualTo(firstResponse.getBody().data())
            );
        }
    }
}
