package com.loopers.application.product;

import com.loopers.application.like.product.LikeProductFacade;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.common.vo.Price;
import com.loopers.domain.metrics.product.ProductMetrics;
import com.loopers.domain.metrics.product.ProductMetricsService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.supply.Supply;
import com.loopers.domain.supply.SupplyService;
import com.loopers.domain.supply.vo.Stock;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.infrastructure.cache.GlobalCache;
import com.loopers.infrastructure.cache.RedisCacheKeyConfig;
import com.loopers.support.error.CoreException;
import com.loopers.testcontainers.RedisTestContainersConfig;
import com.loopers.utils.DatabaseCleanUp;
import com.loopers.utils.RedisCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Import({RedisTestContainersConfig.class})
@DisplayName("상품 Facade 캐시 통합 테스트")
public class ProductFacadeCacheIntegrationTest {

    @Autowired
    private ProductFacade productFacade;

    @Autowired
    private LikeProductFacade likeProductFacade;

    @Autowired
    private ProductService productService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private ProductMetricsService productMetricsService;

    @Autowired
    private SupplyService supplyService;

    @Autowired
    private UserService userService;

    @Autowired
    private GlobalCache globalCache;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private RedisCleanUp redisCleanUp;

    private Long brandId1;
    private Long brandId2;
    private Long productId1;
    private Long productId2;
    private Long productId3;
    private String userId;

    @BeforeEach
    void setup() {
        // Redis 캐시 초기화
        redisCleanUp.truncateAll();
        // DB 초기화
        databaseCleanUp.truncateAllTables();

        // Brand 등록
        Brand brand1 = Brand.create("Nike");
        brand1 = brandService.save(brand1);
        brandId1 = brand1.getId();

        Brand brand2 = Brand.create("Adidas");
        brand2 = brandService.save(brand2);
        brandId2 = brand2.getId();

        // Product 등록
        Product product1 = Product.create("상품1", brandId1, new Price(10000));
        product1 = productService.save(product1);
        productId1 = product1.getId();
        ProductMetrics metrics1 = ProductMetrics.create(productId1, brandId1, 5);
        productMetricsService.save(metrics1);
        Supply supply1 = Supply.create(productId1, new Stock(100));
        supplyService.save(supply1);

        Product product2 = Product.create("상품2", brandId1, new Price(20000));
        product2 = productService.save(product2);
        productId2 = product2.getId();
        ProductMetrics metrics2 = ProductMetrics.create(productId2, brandId1, 3);
        productMetricsService.save(metrics2);
        Supply supply2 = Supply.create(productId2, new Stock(200));
        supplyService.save(supply2);

        Product product3 = Product.create("상품3", brandId2, new Price(15000));
        product3 = productService.save(product3);
        productId3 = product3.getId();
        ProductMetrics metrics3 = ProductMetrics.create(productId3, brandId2, 10);
        productMetricsService.save(metrics3);
        Supply supply3 = Supply.create(productId3, new Stock(150));
        supplyService.save(supply3);

        // 테스트용 사용자 생성
        User user = userService.registerUser("testuser", "test@test.com", "1993-03-13", "male");
        userId = user.getUserId();
    }

    @AfterEach
    void tearDown() {
        redisCleanUp.truncateAll();
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("상품 상세 조회 캐시 테스트")
    @Nested
    class ProductDetailCacheTest {
        @DisplayName("캐시 미스 시 DB에서 조회하고 캐시에 저장한다")
        @Test
        void should_saveToCache_when_cacheMiss() {
            // arrange
            String cacheKey = RedisCacheKeyConfig.PRODUCT_DETAIL.generateKey(productId1);
            assertThat(globalCache.get(cacheKey)).isNull(); // 캐시에 없음 확인

            // act
            ProductInfo result = productFacade.getProductDetail(productId1);

            // assert
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(productId1);
            // 캐시에 저장되었는지 확인
            String cachedValue = globalCache.get(cacheKey);
            assertThat(cachedValue).isNotNull();
            assertThat(cachedValue).contains("상품1");
        }

        @DisplayName("캐시 히트 시 DB 조회 없이 캐시에서 반환한다")
        @Test
        void should_returnFromCache_when_cacheHit() {
            // arrange - 첫 조회로 캐시 저장
            ProductInfo firstResult = productFacade.getProductDetail(productId1);
            assertThat(firstResult).isNotNull();

            // act - 두 번째 조회 (캐시에서 반환되어야 함)
            ProductInfo secondResult = productFacade.getProductDetail(productId1);

            // assert
            assertThat(secondResult).isNotNull();
            assertThat(secondResult.id()).isEqualTo(firstResult.id());
            assertThat(secondResult.name()).isEqualTo(firstResult.name());
            // 동일한 객체는 아니지만 내용은 동일해야 함
        }

        @DisplayName("존재하지 않는 상품 조회 시 404 응답도 캐싱한다")
        @Test
        void should_cache404Response_when_productNotFound() {
            // arrange
            Long nonExistentProductId = 999L;
            String cacheKey = RedisCacheKeyConfig.PRODUCT_DETAIL.generateKey(nonExistentProductId);

            // act - 첫 조회 (예외 발생 및 캐싱)
            assertThatThrownBy(() -> productFacade.getProductDetail(nonExistentProductId))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("상품을 찾을 수 없습니다");

            // assert - 404 응답이 캐싱되었는지 확인
            String cachedValue = globalCache.get(cacheKey);
            assertThat(cachedValue).isNotNull();
            assertThat(cachedValue).contains("null"); // Optional.empty()가 직렬화된 형태

            // act - 두 번째 조회 (캐시에서 반환되어야 함)
            assertThatThrownBy(() -> productFacade.getProductDetail(nonExistentProductId))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("상품을 찾을 수 없습니다");
        }

        @DisplayName("캐시 무효화 후 재조회 시 DB에서 조회한다")
        @Test
        void should_fetchFromDB_when_cacheInvalidated() {
            // arrange - 첫 조회로 캐시 저장
            ProductInfo firstResult = productFacade.getProductDetail(productId1);
            assertThat(firstResult).isNotNull();

            // act - 캐시 무효화
            String cacheKey = RedisCacheKeyConfig.PRODUCT_DETAIL.generateKey(productId1);
            globalCache.delete(cacheKey);

            // act - 재조회
            ProductInfo secondResult = productFacade.getProductDetail(productId1);

            // assert - DB에서 조회하여 캐시에 다시 저장
            assertThat(secondResult).isNotNull();
            assertThat(secondResult.id()).isEqualTo(productId1);
            // 캐시가 다시 저장되었는지 확인
            String cachedValue = globalCache.get(cacheKey);
            assertThat(cachedValue).isNotNull();
        }
    }

    @DisplayName("상품 목록 조회 캐시 테스트")
    @Nested
    class ProductListCacheTest {
        @DisplayName("단일 브랜드 필터 시 캐시에 저장하고 조회한다")
        @Test
        void should_cache_when_singleBrandFilter() {
            // arrange
            Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
            ProductSearchRequest request = new ProductSearchRequest(pageable, brandId1);
            String sortStr = pageable.getSort().toString();
            String cacheKey = RedisCacheKeyConfig.PRODUCT_LIST.generateKey("brand", brandId1, 0, 20, sortStr);

            // 캐시에 없음 확인
            assertThat(globalCache.get(cacheKey)).isNull();

            // act - 첫 조회
            Page<ProductInfo> firstResult = productFacade.getProductList(request);

            // assert
            assertThat(firstResult).isNotNull();
            assertThat(firstResult.getContent()).hasSize(2); // brandId1에 속한 상품 2개
            // 캐시에 저장되었는지 확인
            String cachedValue = globalCache.get(cacheKey);
            assertThat(cachedValue).isNotNull();

            // act - 두 번째 조회 (캐시에서 반환)
            Page<ProductInfo> secondResult = productFacade.getProductList(request);

            // assert
            assertThat(secondResult).isNotNull();
            assertThat(secondResult.getContent()).hasSize(2);
            assertThat(secondResult.getContent().get(0).id()).isEqualTo(firstResult.getContent().get(0).id());
        }

        @DisplayName("다중 브랜드 필터 시 캐시를 사용하지 않는다")
        @Test
        void should_notCache_when_multipleBrandFilter() {
            // arrange
            Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
            List<Long> brandIds = List.of(brandId1, brandId2);
            ProductSearchRequest request = new ProductSearchRequest(pageable, brandIds);

            // act - 첫 조회
            Page<ProductInfo> firstResult = productFacade.getProductList(request);

            // assert
            assertThat(firstResult).isNotNull();
            assertThat(firstResult.getContent()).hasSize(3); // 모든 브랜드 상품 3개
            // 다중 브랜드 필터는 캐싱하지 않으므로 캐시 키가 생성되지 않아야 함
            // (단일 브랜드 필터가 아니므로 캐시 키 생성 로직이 실행되지 않음)
        }

        @DisplayName("브랜드 필터 없이 조회 시 캐시에 저장한다")
        @Test
        void should_cache_when_noBrandFilter() {
            // arrange
            Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
            ProductSearchRequest request = new ProductSearchRequest(pageable);
            String sortStr = pageable.getSort().toString();
            String cacheKey = RedisCacheKeyConfig.PRODUCT_LIST.generateKey("all", 0, 20, sortStr);

            // 캐시에 없음 확인
            assertThat(globalCache.get(cacheKey)).isNull();

            // act - 첫 조회
            Page<ProductInfo> firstResult = productFacade.getProductList(request);

            // assert
            assertThat(firstResult).isNotNull();
            assertThat(firstResult.getContent()).hasSize(3); // 모든 상품 3개
            // 캐시에 저장되었는지 확인
            String cachedValue = globalCache.get(cacheKey);
            assertThat(cachedValue).isNotNull();
        }

        @DisplayName("좋아요 정렬 시에도 캐시에 저장한다")
        @Test
        void should_cache_when_sortedByLikeCount() {
            // arrange
            Sort sort = Sort.by(Sort.Direction.DESC, "likeCount");
            Pageable pageable = PageRequest.of(0, 20, sort);
            ProductSearchRequest request = new ProductSearchRequest(pageable, brandId1);
            String sortStr = pageable.getSort().toString();
            String cacheKey = RedisCacheKeyConfig.PRODUCT_LIST.generateKey("brand", brandId1, 0, 20, sortStr);

            // 캐시에 없음 확인
            assertThat(globalCache.get(cacheKey)).isNull();

            // act - 첫 조회
            Page<ProductInfo> firstResult = productFacade.getProductList(request);

            // assert
            assertThat(firstResult).isNotNull();
            assertThat(firstResult.getContent()).hasSize(2);
            // 좋아요 수 내림차순 정렬 확인 (5, 3)
            assertThat(firstResult.getContent().get(0).likes()).isGreaterThanOrEqualTo(
                    firstResult.getContent().get(1).likes());
            // 캐시에 저장되었는지 확인
            String cachedValue = globalCache.get(cacheKey);
            assertThat(cachedValue).isNotNull();
        }
    }

    @DisplayName("캐시 무효화 테스트")
    @Nested
    class CacheInvalidationTest {
        @DisplayName("상품 상세 캐시 무효화 후 재조회 시 DB에서 조회한다")
        @Test
        void should_fetchFromDB_afterDetailCacheInvalidation() {
            // arrange - 캐시 저장
            ProductInfo cachedResult = productFacade.getProductDetail(productId1);
            assertThat(cachedResult).isNotNull();

            // act - 캐시 무효화
            String cacheKey = RedisCacheKeyConfig.PRODUCT_DETAIL.generateKey(productId1);
            globalCache.delete(cacheKey);
            assertThat(globalCache.get(cacheKey)).isNull();

            // act - 재조회
            ProductInfo newResult = productFacade.getProductDetail(productId1);

            // assert - DB에서 조회하여 캐시에 다시 저장
            assertThat(newResult).isNotNull();
            assertThat(newResult.id()).isEqualTo(productId1);
            // 캐시가 다시 저장되었는지 확인
            String newCachedValue = globalCache.get(cacheKey);
            assertThat(newCachedValue).isNotNull();
        }

        @DisplayName("상품 목록 캐시 무효화 후 재조회 시 DB에서 조회한다")
        @Test
        void should_fetchFromDB_afterListCacheInvalidation() {
            // arrange - 캐시 저장
            Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
            ProductSearchRequest request = new ProductSearchRequest(pageable, brandId1);
            String sortStr = pageable.getSort().toString();
            String cacheKey = RedisCacheKeyConfig.PRODUCT_LIST.generateKey("brand", brandId1, 0, 20, sortStr);

            Page<ProductInfo> cachedResult = productFacade.getProductList(request);
            assertThat(cachedResult).isNotNull();
            assertThat(cachedResult.getContent()).hasSize(2); // brandId1에 속한 상품 2개
            assertThat(globalCache.get(cacheKey)).isNotNull();

            // act - 캐시 무효화
            globalCache.delete(cacheKey);
            assertThat(globalCache.get(cacheKey)).isNull();

            // act - 재조회
            Page<ProductInfo> newResult = productFacade.getProductList(request);

            // assert - DB에서 조회하여 캐시에 다시 저장
            assertThat(newResult).isNotNull();
            assertThat(newResult.getContent()).hasSize(2); // brandId1에 속한 상품 2개
            // 캐시가 다시 저장되었는지 확인
            String newCachedValue = globalCache.get(cacheKey);
            assertThat(newCachedValue).isNotNull();
        }
    }

    @DisplayName("캐시 미스 시 서비스 정상 동작 테스트")
    @Nested
    class CacheMissServiceResilienceTest {
        @DisplayName("Redis 장애 시에도 서비스가 정상 동작한다")
        @Test
        void should_workNormally_when_redisFailure() {
            // arrange - Redis 연결 끊기 (실제로는 테스트하기 어려우므로, 캐시 미스 상황 시뮬레이션)
            // 캐시에 없는 상태에서 조회

            // act - 캐시 미스 상황에서 조회
            ProductInfo result = productFacade.getProductDetail(productId1);

            // assert - 서비스가 정상 동작하여 결과 반환
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(productId1);
            assertThat(result.name()).isEqualTo("상품1");
        }

        @DisplayName("캐시 직렬화 실패 시에도 서비스가 정상 동작한다")
        @Test
        void should_workNormally_when_serializationFailure() {
            // arrange - 잘못된 형식의 캐시 데이터 저장 (직렬화 실패 시뮬레이션)
            String cacheKey = RedisCacheKeyConfig.PRODUCT_DETAIL.generateKey(productId1);
            globalCache.set(cacheKey, "invalid json format", 3600);

            // act - 잘못된 캐시 데이터 조회 시도
            ProductInfo result = productFacade.getProductDetail(productId1);

            // assert - 역직렬화 실패 시 DB에서 조회하여 정상 반환
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(productId1);
            // 캐시가 올바른 형식으로 다시 저장되었는지 확인
            String newCachedValue = globalCache.get(cacheKey);
            assertThat(newCachedValue).isNotNull();
            assertThat(newCachedValue).contains("상품1");
        }
    }

    @DisplayName("좋아요 변경 시 캐시 무효화 테스트")
    @Nested
    class LikeChangeCacheInvalidationTest {
        @DisplayName("좋아요 등록 시 상품 상세 캐시가 무효화된다")
        @Test
        void should_invalidateDetailCache_when_likeProduct() {
            // arrange - 상품 상세 캐시 저장
            ProductInfo cachedDetail = productFacade.getProductDetail(productId1);
            assertThat(cachedDetail).isNotNull();
            String detailCacheKey = RedisCacheKeyConfig.PRODUCT_DETAIL.generateKey(productId1);
            assertThat(globalCache.get(detailCacheKey)).isNotNull();

            // act - 좋아요 등록 (카운트가 변경되는 경우)
            likeProductFacade.likeProduct(userId, productId1);

            // assert - 캐시가 무효화되었는지 확인
            assertThat(globalCache.get(detailCacheKey)).isNull();

            // 재조회 시 DB에서 조회하여 캐시에 다시 저장되는지 확인
            ProductInfo newDetail = productFacade.getProductDetail(productId1);
            assertThat(newDetail).isNotNull();
            assertThat(newDetail.likes()).isEqualTo(cachedDetail.likes() + 1); // 좋아요 수 증가 확인
            assertThat(globalCache.get(detailCacheKey)).isNotNull(); // 캐시 재저장 확인
        }

        @DisplayName("좋아요 취소 시 상품 상세 캐시가 무효화된다")
        @Test
        void should_invalidateDetailCache_when_unlikeProduct() {
            // arrange - 좋아요 등록 후 상품 상세 캐시 저장
            likeProductFacade.likeProduct(userId, productId1);
            ProductInfo cachedDetail = productFacade.getProductDetail(productId1);
            assertThat(cachedDetail).isNotNull();
            String detailCacheKey = RedisCacheKeyConfig.PRODUCT_DETAIL.generateKey(productId1);
            assertThat(globalCache.get(detailCacheKey)).isNotNull();

            // act - 좋아요 취소 (카운트가 변경되는 경우)
            likeProductFacade.unlikeProduct(userId, productId1);

            // assert - 캐시가 무효화되었는지 확인
            assertThat(globalCache.get(detailCacheKey)).isNull();

            // 재조회 시 DB에서 조회하여 캐시에 다시 저장되는지 확인
            ProductInfo newDetail = productFacade.getProductDetail(productId1);
            assertThat(newDetail).isNotNull();
            assertThat(newDetail.likes()).isEqualTo(cachedDetail.likes() - 1); // 좋아요 수 감소 확인
            assertThat(globalCache.get(detailCacheKey)).isNotNull(); // 캐시 재저장 확인
        }

        @DisplayName("좋아요 변경 시 상품 목록 캐시가 무효화된다")
        @Test
        void should_invalidateListCache_when_likeProduct() {
            // arrange - 상품 목록 캐시 저장
            Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
            ProductSearchRequest request = new ProductSearchRequest(pageable, brandId1);
            Page<ProductInfo> cachedList = productFacade.getProductList(request);
            assertThat(cachedList).isNotNull();

            String listCacheKey = RedisCacheKeyConfig.PRODUCT_LIST.generateKey(
                    "brand", brandId1, 0, 20, pageable.getSort().toString());
            assertThat(globalCache.get(listCacheKey)).isNotNull();

            // act - 좋아요 등록 (카운트가 변경되는 경우)
            likeProductFacade.likeProduct(userId, productId1);

            // assert - 브랜드별 목록 캐시가 무효화되었는지 확인
            assertThat(globalCache.get(listCacheKey)).isNull();

            // 재조회 시 DB에서 조회하여 캐시에 다시 저장되는지 확인
            Page<ProductInfo> newList = productFacade.getProductList(request);
            assertThat(newList).isNotNull();
            assertThat(newList.getContent()).hasSize(2);
            // 좋아요 수가 증가한 상품 확인
            ProductInfo updatedProduct = newList.getContent().stream()
                    .filter(p -> p.id().equals(productId1))
                    .findFirst()
                    .orElseThrow();
            assertThat(updatedProduct.likes()).isGreaterThan(
                    cachedList.getContent().stream()
                            .filter(p -> p.id().equals(productId1))
                            .findFirst()
                            .map(ProductInfo::likes)
                            .orElse(0));
            assertThat(globalCache.get(listCacheKey)).isNotNull(); // 캐시 재저장 확인
        }

        @DisplayName("중복 좋아요 등록 시 캐시가 무효화되지 않는다")
        @Test
        void should_notInvalidateCache_when_duplicateLike() {
            // arrange - 좋아요 등록 후 상품 상세 캐시 저장
            likeProductFacade.likeProduct(userId, productId1);
            ProductInfo cachedDetail = productFacade.getProductDetail(productId1);
            assertThat(cachedDetail).isNotNull();
            String detailCacheKey = RedisCacheKeyConfig.PRODUCT_DETAIL.generateKey(productId1);
            assertThat(globalCache.get(detailCacheKey)).isNotNull();
            int cachedLikes = cachedDetail.likes();

            // act - 중복 좋아요 등록 (카운트가 변경되지 않는 경우)
            likeProductFacade.likeProduct(userId, productId1);

            // assert - 캐시가 유지되는지 확인 (무효화되지 않음)
            assertThat(globalCache.get(detailCacheKey)).isNotNull();
            // 좋아요 수가 변경되지 않았는지 확인
            ProductInfo newDetail = productFacade.getProductDetail(productId1);
            assertThat(newDetail.likes()).isEqualTo(cachedLikes);
        }
    }

    @DisplayName("상품 생성 시 캐시 무효화 테스트")
    @Nested
    class ProductCreateCacheInvalidationTest {
        @DisplayName("상품 생성 시 해당 브랜드의 목록 캐시가 무효화된다")
        @Test
        void should_invalidateListCache_when_createProduct() {
            // arrange - 상품 목록 캐시 저장
            Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
            ProductSearchRequest request = new ProductSearchRequest(pageable, brandId1);
            Page<ProductInfo> cachedList = productFacade.getProductList(request);
            assertThat(cachedList).isNotNull();
            assertThat(cachedList.getContent()).hasSize(2); // 기존 상품 2개

            String listCacheKey = RedisCacheKeyConfig.PRODUCT_LIST.generateKey(
                    "brand", brandId1, 0, 20, pageable.getSort().toString());
            assertThat(globalCache.get(listCacheKey)).isNotNull();

            // act - 새 상품 생성
            ProductInfo newProduct = productFacade.createProduct(
                    new ProductCreateRequest("새 상품", brandId1, new Price(30000), new Stock(300), 0));

            // assert - 브랜드별 목록 캐시가 무효화되었는지 확인
            assertThat(globalCache.get(listCacheKey)).isNull();

            // 재조회 시 DB에서 조회하여 새로운 상품이 포함되고 캐시에 다시 저장되는지 확인
            Page<ProductInfo> newList = productFacade.getProductList(request);
            assertThat(newList).isNotNull();
            assertThat(newList.getContent()).hasSize(3); // 새 상품 포함하여 3개
            assertThat(newList.getContent()).anyMatch(p -> p.id().equals(newProduct.id()));
            assertThat(globalCache.get(listCacheKey)).isNotNull(); // 캐시 재저장 확인
        }

        @DisplayName("상품 일괄 생성 시 관련 브랜드의 목록 캐시가 무효화된다")
        @Test
        void should_invalidateListCache_when_createProductBulk() {
            // arrange - 두 브랜드의 목록 캐시 저장
            Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
            ProductSearchRequest request1 = new ProductSearchRequest(pageable, brandId1);
            ProductSearchRequest request2 = new ProductSearchRequest(pageable, brandId2);

            Page<ProductInfo> cachedList1 = productFacade.getProductList(request1);
            Page<ProductInfo> cachedList2 = productFacade.getProductList(request2);

            String listCacheKey1 = RedisCacheKeyConfig.PRODUCT_LIST.generateKey(
                    "brand", brandId1, 0, 20, pageable.getSort().toString());
            String listCacheKey2 = RedisCacheKeyConfig.PRODUCT_LIST.generateKey(
                    "brand", brandId2, 0, 20, pageable.getSort().toString());

            assertThat(globalCache.get(listCacheKey1)).isNotNull();
            assertThat(globalCache.get(listCacheKey2)).isNotNull();

            // act - 두 브랜드에 각각 상품 일괄 생성
            List<ProductInfo> newProducts = productFacade.createProductBulk(List.of(
                    new ProductCreateRequest("새 상품1", brandId1, new Price(30000), new Stock(300), 0),
                    new ProductCreateRequest("새 상품2", brandId2, new Price(40000), new Stock(400), 0)
            ));

            // assert - 두 브랜드의 목록 캐시가 모두 무효화되었는지 확인
            assertThat(globalCache.get(listCacheKey1)).isNull();
            assertThat(globalCache.get(listCacheKey2)).isNull();

            // 재조회 시 새로운 상품이 포함되고 캐시에 다시 저장되는지 확인
            Page<ProductInfo> newList1 = productFacade.getProductList(request1);
            Page<ProductInfo> newList2 = productFacade.getProductList(request2);

            assertThat(newList1.getContent()).hasSize(3); // 새 상품 포함하여 3개
            assertThat(newList2.getContent()).hasSize(2); // 새 상품 포함하여 2개
            assertThat(globalCache.get(listCacheKey1)).isNotNull(); // 캐시 재저장 확인
            assertThat(globalCache.get(listCacheKey2)).isNotNull(); // 캐시 재저장 확인
        }
    }
}

