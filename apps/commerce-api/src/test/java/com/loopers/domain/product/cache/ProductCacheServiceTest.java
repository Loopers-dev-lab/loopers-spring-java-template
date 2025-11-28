package com.loopers.domain.product.cache;

import com.loopers.domain.product.ProductStatus;
import com.loopers.domain.product.view.ProductCondition;
import com.loopers.domain.product.view.ProductView;
import com.loopers.domain.product.view.ProductViewRepository;
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
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProductCacheService 통합 테스트")
@SpringBootTest
@Import(RedisTestContainersConfig.class)
class ProductCacheServiceTest {

    @Autowired
    private ProductCacheService productCacheService;
    
    @Autowired
    private ProductViewRepository productViewRepository;
    
    @Autowired
    private RedisTemplate<String, Object> productCacheRedisTemplate;
    
    @Autowired
    private RedisTemplate<String, String> productListCacheRedisTemplate;
    
    @Autowired
    private DatabaseCleanUp databaseCleanUp;
    
    @Autowired
    private RedisCleanUp redisCleanUp;

    @AfterEach
    void tearDown() {
        redisCleanUp.truncateAll();
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("Hot 판단 테스트")
    @Nested
    class HotDetectionTest {

        @DisplayName("brandId가 1~500 범위에 있으면 Hot Brand로 판단한다")
        @Test
        void isHotBrand_withHotBrandId_returnsTrue() {
            // act & assert
            assertThat(productCacheService.isHotBrand(1L)).isTrue();
            assertThat(productCacheService.isHotBrand(250L)).isTrue();
            assertThat(productCacheService.isHotBrand(500L)).isTrue();
        }

        @DisplayName("brandId가 500 초과이면 Cold Brand로 판단한다")
        @Test
        void isHotBrand_withColdBrandId_returnsFalse() {
            // act & assert
            assertThat(productCacheService.isHotBrand(501L)).isFalse();
            assertThat(productCacheService.isHotBrand(1000L)).isFalse();
        }

        @DisplayName("productId가 1~5000 범위에 있으면 Hot Product로 판단한다")
        @Test
        void isHotProduct_withHotProductId_returnsTrue() {
            // act & assert
            assertThat(productCacheService.isHotProduct(1L)).isTrue();
            assertThat(productCacheService.isHotProduct(2500L)).isTrue();
            assertThat(productCacheService.isHotProduct(5000L)).isTrue();
        }

        @DisplayName("productId가 5000 초과이면 Cold Product로 판단한다")
        @Test
        void isHotProduct_withColdProductId_returnsFalse() {
            // act & assert
            assertThat(productCacheService.isHotProduct(5001L)).isFalse();
            assertThat(productCacheService.isHotProduct(10000L)).isFalse();
        }
    }

    @DisplayName("Hot Page 캐싱 테스트")
    @Nested
    class HotPageCacheTest {

        @DisplayName("Hot Page 캐시 히트 시 DB 조회하지 않는다")
        @Test
        void hotPage_cacheHit_doesNotQueryDb() throws InterruptedException {
            // arrange
            createAndSaveProductView(1L, "상품1");
            createAndSaveProductView(2L, "상품2");
            createAndSaveProductView(3L, "상품3");
            
            ProductCondition condition = ProductCondition.builder().sort("latest").build();
            Pageable pageable = PageRequest.of(0, 10); // Hot Page (page 0, brandId 없음)
            
            // 첫 번째 조회로 캐시 웜업
            productCacheService.getProductViews(condition, pageable);
            
            // DB 데이터 변경 (캐시와 다른 값으로)
            productViewRepository.update(1L, "변경된 상품1", BigDecimal.valueOf(20000), null, null, ProductStatus.ON_SALE);
            
            // act - 두 번째 조회 (캐시 히트)
            Page<ProductView> result = productCacheService.getProductViews(condition, pageable);
            
            // assert - 캐시된 값이 반환됨 (변경 전 값)
            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getContent().get(2).getName()).isEqualTo("상품1"); // 캐시된 값
        }

        @DisplayName("Hot Page 캐시 미스 시 DB 조회 후 Backfill한다")
        @Test
        void hotPage_cacheMiss_queriesDbAndBackfills() {
            // arrange
            createAndSaveProductView(1L, "상품1");
            createAndSaveProductView(2L, "상품2");
            
            ProductCondition condition = ProductCondition.builder().sort("latest").build();
            Pageable pageable = PageRequest.of(0, 10); // Hot Page (page 0)
            
            // act - 첫 번째 조회 (캐시 미스, DB 조회 후 Backfill)
            Page<ProductView> result = productCacheService.getProductViews(condition, pageable);
            
            // assert
            assertThat(result.getContent()).hasSize(2);
            
            // L1 캐시 확인
            String listKey = "products::list::sort:latest::page:0::size:10";
            String cachedIds = productListCacheRedisTemplate.opsForValue().get(listKey);
            assertThat(cachedIds).isNotNull(); // PER 적용된 저장
            
            // L2 캐시 확인 (Backfill로 저장됨)
            Object cachedInfo1 = productCacheRedisTemplate.opsForValue().get("product:info:1");
            Object cachedInfo2 = productCacheRedisTemplate.opsForValue().get("product:info:2");
            assertThat(cachedInfo1).isNotNull(); // Backfill로 저장됨
            assertThat(cachedInfo2).isNotNull(); // Backfill로 저장됨
        }

        @DisplayName("Hot Page L2 부분 미스 시 Backfill한다")
        @Test
        void hotPage_partialL2Miss_backfillsFromDb() {
            // arrange
            createAndSaveProductView(1L, "상품1");
            createAndSaveProductView(2L, "상품2");
            createAndSaveProductView(3L, "상품3");
            
            ProductCondition condition = ProductCondition.builder().sort("latest").build();
            Pageable pageable = PageRequest.of(0, 10); // Hot Page
            
            // 첫 번째 조회로 L1, L2 캐시 웜업
            productCacheService.getProductViews(condition, pageable);
            
            // L2 Info 캐시만 삭제 (ID 3만)
            productCacheRedisTemplate.delete("product:info:3");
            
            // act - 두 번째 조회 (L1 히트, L2 부분 미스)
            Page<ProductView> result = productCacheService.getProductViews(condition, pageable);
            
            // assert
            assertThat(result.getContent()).hasSize(3);
            
            // ID 3의 L2 캐시가 Backfill로 저장되었는지 확인
            Object cachedInfo3 = productCacheRedisTemplate.opsForValue().get("product:info:3");
            assertThat(cachedInfo3).isNotNull(); // Backfill로 저장됨
        }
    }

    @DisplayName("Cold Page 캐싱 테스트")
    @Nested
    class ColdPageCacheTest {

        @DisplayName("Cold Page 캐시 미스 시 Look Aside로 L1, L2 캐시 모두 저장된다")
        @Test
        void coldPage_cacheMiss_savesBothL1AndL2WithLookAside() {
            // arrange
            // Cold Brand(1000L)로 ProductView 생성 (page=5에서 조회하려면 최소 50개 이상 필요)
            // 60개 생성하여 page=5 (offset 50)에서도 데이터가 나오도록 함
            for (long i = 1; i <= 60; i++) {
                createAndSaveProductView(i, "테스트 상품 " + i, 1000L);
            }
            ProductCondition condition = ProductCondition.builder()
                    .brandId(1000L) // Cold Brand
                    .sort("latest")
                    .build();
            Pageable pageable = PageRequest.of(5, 10); // Cold Page (page 5)
            
            // act
            Page<ProductView> result = productCacheService.getProductViews(condition, pageable);
            
            // assert
            assertThat(result.getContent()).hasSize(10); // page=5, size=10이므로 10개 반환
            
            // L1 캐시는 Look Aside로 저장됨
            String listKey = "products::list::brand:1000::sort:latest::page:5::size:10";
            String cachedIds = productListCacheRedisTemplate.opsForValue().get(listKey);
            assertThat(cachedIds).isNotNull(); // Look Aside로 캐시 저장됨
            
            // L2 캐시도 Look Aside로 저장됨 (Cold Page에서도 getProductViews 호출 시 저장)
            // 결과에 포함된 첫 번째 상품의 ID를 확인
            Long firstProductId = result.getContent().get(0).getId();
            Object cachedInfo = productCacheRedisTemplate.opsForValue().get("product:info:" + firstProductId);
            assertThat(cachedInfo).isNotNull(); // Look Aside로 캐시 저장됨
            
            // 참고: Cold Page에서 이벤트 발생 시 자동 Backfill은 없음
            // (이것은 ProductViewEventHandler 테스트에서 확인)
        }
    }

    @DisplayName("L2 캐시 테스트")
    @Nested
    class L2CacheTest {

        @DisplayName("Info와 Stat 캐시를 조합하여 반환한다")
        @Test
        void getProductView_combinesInfoAndStat() {
            // arrange
            Long productId = 1L; // Hot Product (1~5000 범위)
            createAndSaveProductView(productId, "테스트 상품");
            
            // 첫 번째 조회로 캐시 웜업
            productCacheService.getProductView(productId);
            
            // Stat 캐시 직접 수정
            productCacheRedisTemplate.opsForHash().put("product:stat:" + productId, "likeCount", "999");
            
            // act
            Optional<ProductView> result = productCacheService.getProductView(productId);
            
            // assert
            assertThat(result).isPresent();
            assertThat(result.get().getLikeCount()).isEqualTo(999L);
        }
    }

    private ProductView createAndSaveProductView(Long id, String name) {
        return createAndSaveProductView(id, name, 1L);
    }

    private ProductView createAndSaveProductView(Long id, String name, Long brandId) {
        ProductView productView = ProductView.builder()
                .id(id)
                .name(name)
                .price(BigDecimal.valueOf(10000))
                .likeCount(10L)
                .brandId(brandId)
                .brandName("테스트 브랜드")
                .status(ProductStatus.ON_SALE)
                .createdAt(ZonedDateTime.now())
                .build();
        return productViewRepository.save(productView).orElseThrow();
    }
}
