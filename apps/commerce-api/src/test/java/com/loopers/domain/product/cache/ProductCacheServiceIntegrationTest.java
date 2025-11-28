package com.loopers.domain.product.cache;

import com.loopers.domain.product.ProductStatus;
import com.loopers.domain.product.view.ProductCondition;
import com.loopers.domain.product.view.ProductView;
import com.loopers.domain.product.view.ProductViewRepository;
import com.loopers.utils.DatabaseCleanUp;
import com.loopers.utils.RedisCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
class ProductCacheServiceIntegrationTest {

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

    @DisplayName("Hot Page 캐싱 테스트")
    @Nested
    class HotPageTest {

        @DisplayName("Hot Page 첫 요청 시 L1+L2 캐시에 저장한다")
        @Test
        void hotPage_firstRequest_cachesL1AndL2() {
            // arrange
            createAndSaveProductView(1L, "테스트 상품");
            ProductCondition condition = ProductCondition.builder().sort("latest").build();
            Pageable pageable = PageRequest.of(0, 10); // Hot Page (page 0, brandId 없음)

            // act
            Page<ProductView> result = productCacheService.getProductViews(condition, pageable);

            // assert
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("테스트 상품");
            
            // L1 캐시 확인
            String listKey = "products::list::sort:latest::page:0::size:10";
            String cachedIds = productListCacheRedisTemplate.opsForValue().get(listKey);
            assertThat(cachedIds).isNotNull();
            assertThat(cachedIds).contains("1");
            
            // L2 캐시 확인
            Object cachedInfo = productCacheRedisTemplate.opsForValue().get("product:info:1");
            assertThat(cachedInfo).isNotNull();
        }

        @DisplayName("Hot Page 두 번째 요청 시 캐시에서 가져온다")
        @Test
        void hotPage_secondRequest_fetchesFromCache() {
            // arrange
            createAndSaveProductView(1L, "테스트 상품");
            ProductCondition condition = ProductCondition.builder().sort("latest").build();
            Pageable pageable = PageRequest.of(0, 10);

            // 첫 번째 조회 (캐시 웜업)
            productCacheService.getProductViews(condition, pageable);
            
            // DB 데이터 변경 (캐시와 다른 값으로)
            productViewRepository.update(1L, "변경된 상품", BigDecimal.valueOf(20000), null, null, ProductStatus.ON_SALE);

            // act - 두 번째 조회 (캐시 히트)
            Page<ProductView> result = productCacheService.getProductViews(condition, pageable);

            // assert - 캐시된 값이 반환됨 (변경 전 값)
            assertThat(result.getContent().get(0).getName()).isEqualTo("테스트 상품");
        }

        @DisplayName("Hot Page Info 캐시 삭제 후 Backfill하여 최신 데이터를 가져온다")
        @Test
        void hotPage_afterInfoEviction_backfillsLatestData() {
            // arrange
            createAndSaveProductView(1L, "테스트 상품");
            ProductCondition condition = ProductCondition.builder().sort("latest").build();
            Pageable pageable = PageRequest.of(0, 10);

            // 첫 번째 조회 (캐시 웜업)
            productCacheService.getProductViews(condition, pageable);
            
            // DB 데이터 변경
            productViewRepository.update(1L, "변경된 상품", BigDecimal.valueOf(20000), null, null, ProductStatus.ON_SALE);
            
            // L2 Info 캐시만 삭제 (Evict 시뮬레이션)
            productCacheRedisTemplate.delete("product:info:1");

            // act
            Page<ProductView> result = productCacheService.getProductViews(condition, pageable);

            // assert - 최신 DB 데이터로 Backfill됨
            assertThat(result.getContent().get(0).getName()).isEqualTo("변경된 상품");
        }
    }

    @DisplayName("Cold Page 캐싱 테스트")
    @Nested
    class ColdPageTest {

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
            Pageable pageable = PageRequest.of(5, 10); // Cold Page (page 5, hotPageRange=3이므로 0~2는 Hot)

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

    @DisplayName("Stat 캐시 테스트")
    @Nested
    class StatCacheTest {

        @DisplayName("좋아요 수는 product:stat에서 가져온다")
        @Test
        void likeCount_fetchedFromStatCache() {
            // arrange
            createAndSaveProductView(1L, "테스트 상품");
            
            // 첫 번째 조회로 캐시 웜업
            productCacheService.getProductView(1L);
            
            // Stat 캐시 직접 수정
            productCacheRedisTemplate.opsForHash().put("product:stat:1", "likeCount", "999");

            // act
            Optional<ProductView> result = productCacheService.getProductView(1L);

            // assert
            assertThat(result).isPresent();
            assertThat(result.get().getLikeCount()).isEqualTo(999L);
        }
    }

    @DisplayName("Hot/Cold TTL 테스트")
    @Nested
    class HotColdTtlTest {

        @DisplayName("Hot Page는 PER이 적용된 TTL로 저장된다")
        @Test
        void hotPage_savedWithPERTtl() {
            // arrange
            createAndSaveProductView(1L, "테스트 상품");
            ProductCondition condition = ProductCondition.builder().sort("latest").build();
            Pageable pageable = PageRequest.of(0, 10); // Hot Page (page 0)

            // act
            productCacheService.getProductViews(condition, pageable);

            // assert
            String listKey = "products::list::sort:latest::page:0::size:10";
            Long ttl = productListCacheRedisTemplate.getExpire(listKey);
            
            // PER 적용으로 TTL이 300~360초 범위 (baseTtl + 0~60초 랜덤)
            assertThat(ttl).isBetween(295L, 365L);
        }

        @DisplayName("Cold Page는 기본 TTL로 저장된다")
        @Test
        void coldPage_savedWithBaseTtl() {
            // arrange
            createAndSaveProductView(1L, "테스트 상품");
            ProductCondition condition = ProductCondition.builder()
                    .brandId(1000L) // Cold Brand
                    .sort("latest")
                    .build();
            Pageable pageable = PageRequest.of(5, 10); // Cold Page (page 5)

            // act
            productCacheService.getProductViews(condition, pageable);

            // assert
            String listKey = "products::list::brand:1000::sort:latest::page:5::size:10";
            Long ttl = productListCacheRedisTemplate.getExpire(listKey);
            
            // 기본 TTL 300초
            assertThat(ttl).isLessThanOrEqualTo(300L);
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

