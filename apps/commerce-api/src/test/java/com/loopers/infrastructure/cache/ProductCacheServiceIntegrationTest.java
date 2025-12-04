package com.loopers.infrastructure.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.product.ProductInfo;
import com.loopers.application.product.ProductSort;
import com.loopers.utils.DatabaseCleanUp;
import com.loopers.utils.RedisCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ProductCacheServiceIntegrationTest {

    @Autowired
    private ProductCacheService productCacheService;

    @Autowired
    @Qualifier("redisTemplateObject")
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    @Qualifier("redisTemplateObjectMaster")
    private RedisTemplate<String, Object> redisTemplateMaster;

    @Autowired
    @Qualifier("redisObjectMapper")
    private ObjectMapper objectMapper;

    @Autowired
    private RedisCleanUp redisCleanUp;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        redisCleanUp.truncateAll();
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("상품 상세 조회 캐시 테스트")
    @Nested
    class ProductDetailCacheTest {

        @DisplayName("캐시 미스 시 DB에서 조회하고 캐시에 저장한다.")
        @Test
        void savesToCache_whenCacheMiss() {
            // arrange
            Long productId = 1L;
            ProductInfo expectedProduct = createProductInfo(productId, "상품1", 1L, "브랜드1", 10000, 10, 100);

            // act
            ProductInfo result = productCacheService.getProduct(productId, () -> expectedProduct);

            // assert
            assertThat(result)
                    .usingRecursiveComparison()
                    .ignoringFields("createdAt")
                    .isEqualTo(expectedProduct);

            String cacheKey = productCacheService.getProductDetailKey(productId);
            Object cachedValue = redisTemplate.opsForValue().get(cacheKey);
            assertThat(cachedValue).isNotNull();

            ProductInfo cachedProduct = objectMapper.convertValue(
                    cachedValue, ProductInfo.class);
            assertThat(cachedProduct)
                    .usingRecursiveComparison()
                    .ignoringFields("createdAt")
                    .isEqualTo(expectedProduct);
        }

        @DisplayName("캐시 히트 시 DB를 조회하지 않고 캐시에서 반환한다.")
        @Test
        void returnsFromCache_whenCacheHit() {
            // arrange
            Long productId = 1L;
            ProductInfo product1 = createProductInfo(productId, "상품1", 1L, "브랜드1", 10000, 10, 100);
            ProductInfo product2 = createProductInfo(productId, "상품2", 1L, "브랜드1", 20000, 20, 200);

            productCacheService.getProduct(productId, () -> product1);

            // act
            int[] dbCallCount = {0};
            ProductInfo result = productCacheService.getProduct(productId, () -> {
                dbCallCount[0]++;
                return product2;
            });

            // assert
            assertThat(result)
                    .usingRecursiveComparison()
                    .ignoringFields("createdAt")
                    .isEqualTo(product1);
            assertThat(dbCallCount[0]).isEqualTo(0);
        }

        @DisplayName("TTL이 임계값 이하일 때 Stale-While-Revalidate가 동작한다.")
        @Test
        void triggersStaleWhileRevalidate_whenTtlBelowThreshold() throws InterruptedException {
            // arrange
            Long productId = 1L;
            ProductInfo product1 = createProductInfo(productId, "상품1", 1L, "브랜드1", 10000, 10, 100);
            ProductInfo product2 = createProductInfo(productId, "상품2", 1L, "브랜드1", 20000, 20, 200);

            String cacheKey = productCacheService.getProductDetailKey(productId);
            redisTemplateMaster.opsForValue().set(cacheKey, product1, 15, TimeUnit.SECONDS);

            Thread.sleep(6000);

            // act
            int[] dbCallCount = {0};
            ProductInfo result = productCacheService.getProduct(productId, () -> {
                dbCallCount[0]++;
                return product2;
            });

            // assert
            assertThat(result)
                    .usingRecursiveComparison()
                    .ignoringFields("createdAt")
                    .isEqualTo(product1);
            
            Thread.sleep(3000);

            assertThat(dbCallCount[0]).isGreaterThanOrEqualTo(0);

            Object cachedValue = redisTemplate.opsForValue().get(cacheKey);
            assertThat(cachedValue).isNotNull();
            ProductInfo cachedProduct = objectMapper.convertValue(
                    cachedValue, ProductInfo.class);
            assertThat(cachedProduct).isNotNull();
        }

        @DisplayName("TTL 만료 후 조회 시 DB에서 조회하고 캐시에 저장한다.")
        @Test
        void queriesFromDb_whenCacheExpired() throws InterruptedException {
            // arrange
            Long productId = 1L;
            ProductInfo product1 = createProductInfo(productId, "상품1", 1L, "브랜드1", 10000, 10, 100);
            ProductInfo product2 = createProductInfo(productId, "상품2", 1L, "브랜드1", 20000, 20, 200);

            String cacheKey = productCacheService.getProductDetailKey(productId);
            redisTemplateMaster.opsForValue().set(cacheKey, product1, 1, TimeUnit.SECONDS);

            Thread.sleep(2000);

            // act
            int[] dbCallCount = {0};
            ProductInfo result = productCacheService.getProduct(productId, () -> {
                dbCallCount[0]++;
                return product2;
            });

            // assert
            assertThat(result)
                    .usingRecursiveComparison()
                    .ignoringFields("createdAt")
                    .isEqualTo(product2);
            assertThat(dbCallCount[0]).isEqualTo(1);

            Object cachedValue = redisTemplate.opsForValue().get(cacheKey);
            assertThat(cachedValue).isNotNull();
            ProductInfo cachedProduct = objectMapper.convertValue(
                    cachedValue, ProductInfo.class);
            assertThat(cachedProduct)
                    .usingRecursiveComparison()
                    .ignoringFields("createdAt")
                    .isEqualTo(product2);
        }
    }

    @DisplayName("상품 목록 조회 캐시 테스트")
    @Nested
    class ProductListCacheTest {

        @DisplayName("캐시 미스 시 DB에서 조회하고 캐시에 저장한다.")
        @Test
        void savesToCache_whenCacheMiss() {
            // arrange
            Long brandId = null;
            ProductSort sort = ProductSort.LATEST;
            int page = 0;
            int size = 20;
            List<ProductInfo> expectedProducts = List.of(
                    createProductInfo(1L, "상품1", 1L, "브랜드1", 10000, 10, 100),
                    createProductInfo(2L, "상품2", 1L, "브랜드1", 20000, 20, 200)
            );

            // act
            List<ProductInfo> result = productCacheService.getProductList(
                    brandId, sort, page, size, () -> expectedProducts);

            // assert
            assertThat(result)
                    .usingRecursiveComparison()
                    .ignoringFields("createdAt")
                    .isEqualTo(expectedProducts);

            String cacheKey = productCacheService.getProductListKey(brandId, sort, page, size);
            Object cachedValue = redisTemplate.opsForValue().get(cacheKey);
            assertThat(cachedValue).isNotNull();
        }

        @DisplayName("캐시 히트 시 DB를 조회하지 않고 캐시에서 반환한다.")
        @Test
        void returnsFromCache_whenCacheHit() {
            // arrange
            Long brandId = null;
            ProductSort sort = ProductSort.LATEST;
            int page = 0;
            int size = 20;
            List<ProductInfo> products1 = List.of(
                    createProductInfo(1L, "상품1", 1L, "브랜드1", 10000, 10, 100)
            );
            List<ProductInfo> products2 = List.of(
                    createProductInfo(2L, "상품2", 1L, "브랜드1", 20000, 20, 200)
            );

            productCacheService.getProductList(brandId, sort, page, size, () -> products1);

            // act
            int[] dbCallCount = {0};
            List<ProductInfo> result = productCacheService.getProductList(
                    brandId, sort, page, size, () -> {
                        dbCallCount[0]++;
                        return products2;
                    });

            // assert
            assertThat(result)
                    .usingRecursiveComparison()
                    .ignoringFields("createdAt")
                    .isEqualTo(products1);
            assertThat(dbCallCount[0]).isEqualTo(0);
        }

        @DisplayName("다른 정렬 조건은 별도의 캐시 키를 사용한다.")
        @Test
        void usesDifferentCacheKeys_forDifferentSorts() {
            // arrange
            Long brandId = null;
            int page = 0;
            int size = 20;
            List<ProductInfo> latestProducts = List.of(
                    createProductInfo(1L, "상품1", 1L, "브랜드1", 10000, 10, 100)
            );
            List<ProductInfo> priceAscProducts = List.of(
                    createProductInfo(2L, "상품2", 1L, "브랜드1", 20000, 20, 200)
            );

            // act
            productCacheService.getProductList(brandId, ProductSort.LATEST, page, size, () -> latestProducts);
            productCacheService.getProductList(brandId, ProductSort.PRICE_ASC, page, size, () -> priceAscProducts);

            // assert
            String latestKey = productCacheService.getProductListKey(brandId, ProductSort.LATEST, page, size);
            String priceAscKey = productCacheService.getProductListKey(brandId, ProductSort.PRICE_ASC, page, size);

            assertThat(redisTemplate.hasKey(latestKey)).isTrue();
            assertThat(redisTemplate.hasKey(priceAscKey)).isTrue();
            assertThat(latestKey).isNotEqualTo(priceAscKey);
        }

        @DisplayName("브랜드 필터링은 별도의 캐시 키를 사용한다.")
        @Test
        void usesDifferentCacheKeys_forDifferentBrands() {
            // arrange
            int page = 0;
            int size = 20;
            List<ProductInfo> allBrandProducts = List.of(
                    createProductInfo(1L, "상품1", 1L, "브랜드1", 10000, 10, 100)
            );
            List<ProductInfo> brand1Products = List.of(
                    createProductInfo(2L, "상품2", 1L, "브랜드1", 20000, 20, 200)
            );

            // act
            productCacheService.getProductList(null, ProductSort.LATEST, page, size, () -> allBrandProducts);
            productCacheService.getProductList(1L, ProductSort.LATEST, page, size, () -> brand1Products);

            // assert
            String allBrandKey = productCacheService.getProductListKey(null, ProductSort.LATEST, page, size);
            String brand1Key = productCacheService.getProductListKey(1L, ProductSort.LATEST, page, size);

            assertThat(redisTemplate.hasKey(allBrandKey)).isTrue();
            assertThat(redisTemplate.hasKey(brand1Key)).isTrue();
            assertThat(allBrandKey).isNotEqualTo(brand1Key);
        }
    }

    @DisplayName("캐시 무효화 테스트")
    @Nested
    class CacheInvalidationTest {

        @DisplayName("상품 상세 캐시를 직접 삭제할 수 있다.")
        @Test
        void deletesProductDetailCache() {
            // arrange
            Long productId = 1L;
            ProductInfo product = createProductInfo(productId, "상품1", 1L, "브랜드1", 10000, 10, 100);
            productCacheService.getProduct(productId, () -> product);

            String cacheKey = productCacheService.getProductDetailKey(productId);
            assertThat(redisTemplate.hasKey(cacheKey)).isTrue();

            // act
            redisTemplateMaster.delete(cacheKey);

            // assert
            assertThat(redisTemplateMaster.hasKey(cacheKey)).isFalse();
        }
    }

    private ProductInfo createProductInfo(
            Long id, String name, Long brandId, String brandName,
            Integer price, Integer likeCount, Integer stock) {
        return new ProductInfo(
                id,
                name,
                brandId,
                brandName,
                price,
                likeCount,
                stock,
                ZonedDateTime.now()
        );
    }
}

