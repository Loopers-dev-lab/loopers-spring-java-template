package com.loopers.application.product;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.product.ProductCacheService;
import com.loopers.domain.product.ProductMVService;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.dto.ProductSearchFilter;
import com.loopers.fixtures.ProductTestFixture;
import com.loopers.infrastructure.cache.CacheStrategy;
import com.loopers.utils.DatabaseCleanUp;
import com.loopers.utils.RedisCleanUp;

/**
 * 캐싱 전략 통합 테스트
 * <p>
 * Hot/Warm/Cold 데이터 캐싱 전략이 올바르게 작동하는지 검증합니다.
 *
 * @author hyunjikoh
 * @since 2025. 11. 10.
 */
@SpringBootTest
@DisplayName("캐싱 전략 통합 테스트")
public class ProductCacheStrategyIntegrationTest {

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private RedisCleanUp redisCleanUp;

    @Autowired
    private ProductFacade productFacade;

    @Autowired
    private ProductCacheService cacheService;

    @Autowired
    private ProductMVService mvService;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProductRepository productRepository;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
        redisCleanUp.truncateAll();
    }

    @Nested
    @DisplayName("Hot 데이터 캐싱 (첫 2페이지)")
    class HotDataCachingTest {

        @Test
        @DisplayName("전체 상품 목록 1페이지는 HOT, 2페이지는 WARM 전략으로 캐시된다")
        void should_cache_first_page_as_hot_and_second_page_as_warm() {
            // Given: 충분한 상품 데이터 생성 (3페이지 이상)
            ProductTestFixture.createBrandsAndProducts(brandRepository, productRepository, 1, 50);
            mvService.syncMaterializedView();

            // When: 첫 번째 페이지 조회
            Pageable page0 = PageRequest.of(0, 20);
            ProductSearchFilter filter0 = new ProductSearchFilter(null, null, page0);
            productFacade.getProducts(filter0);

            // Then: 캐시에 저장됨 (Hot 전략)
            Optional<List<Long>> cachedIds0 = cacheService.getProductIdsFromCache(
                    CacheStrategy.HOT, null, page0
            );
            assertThat(cachedIds0).isPresent();
            assertThat(cachedIds0.get()).hasSize(20);

            // When: 두 번째 페이지 조회
            Pageable page1 = PageRequest.of(1, 20);
            ProductSearchFilter filter1 = new ProductSearchFilter(null, null, page1);
            Page<ProductInfo> result1 = productFacade.getProducts(filter1);

            // Then: 캐시에 저장됨 (WARM 전략)
            Optional<List<Long>> cachedIds1 = cacheService.getProductIdsFromCache(
                    CacheStrategy.WARM, null, page1
            );
            assertThat(cachedIds1).isPresent();
            assertThat(cachedIds1.get()).hasSize(20);
        }

        @Test
        @DisplayName("브랜드별 상품 목록 첫 페이지는 HOT 전략으로 캐시된다")
        void should_cache_brand_first_2_pages_as_hot_data() {
            // Given: 특정 브랜드의 상품 충분히 생성
            ProductTestFixture.createBrandsAndProducts(brandRepository, productRepository, 1, 50);
            mvService.syncMaterializedView();
            Long brandId = 1L;

            // When: 브랜드별 첫 번째 페이지 조회
            Pageable page0 = PageRequest.of(0, 20);
            ProductSearchFilter filter0 = new ProductSearchFilter(brandId, null, page0);
            Page<ProductInfo> result0 = productFacade.getProducts(filter0);

            // Then: 캐시에 저장됨 (Hot 전략)
            Optional<List<Long>> cachedIds0 = cacheService.getProductIdsFromCache(
                    CacheStrategy.HOT, brandId, page0
            );
            assertThat(cachedIds0).isPresent();

            // When: 브랜드별 두 번째 페이지 조회
            Pageable page1 = PageRequest.of(1, 20);
            ProductSearchFilter filter1 = new ProductSearchFilter(brandId, null, page1);
            Page<ProductInfo> result1 = productFacade.getProducts(filter1);

            // Then: 캐시에 저장됨 (Hot 전략)
            Optional<List<Long>> cachedIds1 = cacheService.getProductIdsFromCache(
                    CacheStrategy.WARM, brandId, page1
            );
            assertThat(cachedIds1).isPresent();
        }

        @Test
        @DisplayName("Hot 데이터는 30분 TTL로 캐시된다")
        void should_cache_hot_data_with_30_minutes_ttl() {
            // Given: 상품 데이터 생성
            ProductTestFixture.createBrandsAndProducts(brandRepository, productRepository, 1, 25);
            mvService.syncMaterializedView();

            // When: 첫 페이지 조회 (Hot 데이터)
            Pageable pageable = PageRequest.of(0, 20);
            ProductSearchFilter filter = new ProductSearchFilter(null, null, pageable);
            productFacade.getProducts(filter);

            // Then: 캐시에 저장되고 TTL이 30분 이하임
            Optional<List<Long>> cachedIds = cacheService.getProductIdsFromCache(
                    CacheStrategy.HOT, null, pageable
            );
            assertThat(cachedIds).isPresent();

            // TODO: Redis TTL 확인 로직 추가
            // TTL이 30분(1800초) 이하인지 검증
        }
    }

    @Nested
    @DisplayName("Warm 데이터 캐싱 (2~3페이지)")
    class WarmDataCachingTest {

        @Test
        @DisplayName("전체 상품 목록 2~3페이지(page index 1~2)는 Warm 전략으로 캐시된다")
        void should_cache_pages_2_to_3_as_warm_data() {
            // Given: 충분한 상품 데이터 생성 (4페이지 이상)
            ProductTestFixture.createBrandsAndProducts(brandRepository, productRepository, 1, 120);
            mvService.syncMaterializedView();

            // When: 2페이지 조회 (page index 1)
            Pageable page2 = PageRequest.of(1, 20);
            ProductSearchFilter filter2 = new ProductSearchFilter(null, null, page2);
            productFacade.getProducts(filter2);

            // Then: Warm 전략으로 캐시됨
            Optional<List<Long>> cachedIds2 = cacheService.getProductIdsFromCache(
                    CacheStrategy.WARM, null, page2
            );
            assertThat(cachedIds2).isPresent();

            // When: 3페이지 조회 (page index 2)
            Pageable page3 = PageRequest.of(2, 20);
            ProductSearchFilter filter3 = new ProductSearchFilter(null, null, page3);
            productFacade.getProducts(filter3);

            // Then: Warm 전략으로 캐시됨
            Optional<List<Long>> cachedIds3 = cacheService.getProductIdsFromCache(
                    CacheStrategy.WARM, null, page3
            );
            assertThat(cachedIds3).isPresent();

            // When: 4페이지 조회 (page index 3)
            Pageable page4 = PageRequest.of(3, 20);
            ProductSearchFilter filter4 = new ProductSearchFilter(null, null, page4);
            productFacade.getProducts(filter4);

            // Then: COLD 전략으로 캐시를 안함
            Optional<List<Long>> cachedIds4 = cacheService.getProductIdsFromCache(
                    CacheStrategy.COLD, null, page4
            );
            assertThat(cachedIds4).isEmpty();
        }

        @Test
        @DisplayName("Warm 데이터는 10분 TTL로 캐시된다")
        void should_cache_warm_data_with_15_minutes_ttl() {
            // Given: 상품 데이터 생성
            ProductTestFixture.createBrandsAndProducts(brandRepository, productRepository, 1, 70);
            mvService.syncMaterializedView();

            // When: 3페이지 조회 (Warm 데이터)
            Pageable pageable = PageRequest.of(2, 20);
            ProductSearchFilter filter = new ProductSearchFilter(null, null, pageable);
            productFacade.getProducts(filter);

            // Then: 캐시에 저장되고 TTL이 15분 이하임
            Optional<List<Long>> cachedIds = cacheService.getProductIdsFromCache(
                    CacheStrategy.WARM, null, pageable
            );
            assertThat(cachedIds).isPresent();

        }
    }

    @Nested
    @DisplayName("Cold 데이터 캐싱 (6페이지 이후)")
    class ColdDataCachingTest {

        @Test
        @DisplayName("6페이지 이후는 Cold 전략으로 캐시된다")
        void should_cache_pages_after_6_as_cold_data() {
            // Given: 충분한 상품 데이터 생성
            ProductTestFixture.createBrandsAndProducts(brandRepository, productRepository, 1, 150);
            mvService.syncMaterializedView();

            // When: 6페이지 조회
            Pageable page5 = PageRequest.of(5, 20);
            ProductSearchFilter filter5 = new ProductSearchFilter(null, null, page5);
            productFacade.getProducts(filter5);

            // Then: Cold 전략은 캐시하지 않음 (캐시 미스 확인)
            Optional<List<Long>> cachedIds5 = cacheService.getProductIdsFromCache(
                    CacheStrategy.COLD, null, page5
            );
            assertThat(cachedIds5).isEmpty();

            // When: 7페이지 조회
            Pageable page6 = PageRequest.of(6, 20);
            ProductSearchFilter filter6 = new ProductSearchFilter(null, null, page6);
            productFacade.getProducts(filter6);

            // Then: Cold 전략은 캐시하지 않음 (캐시 미스 확인)
            Optional<List<Long>> cachedIds6 = cacheService.getProductIdsFromCache(
                    CacheStrategy.COLD, null, page6
            );
            assertThat(cachedIds6).isEmpty();
        }

        @Test
        @DisplayName("검색 결과는 Cold 전략으로 캐시를 안한다.")
        void should_cache_search_results_as_cold_data() {
            // Given: 상품 데이터 생성
            ProductTestFixture.createBrandsAndProducts(brandRepository, productRepository, 1, 30);
            mvService.syncMaterializedView();

            // When: 검색 조회
            Pageable pageable = PageRequest.of(0, 20);
            ProductSearchFilter filter = new ProductSearchFilter(null, "Product", pageable);
            productFacade.getProducts(filter);

            // Then: Cold 전략은 캐시하지 않음 (캐시 미스 확인)
            Optional<List<Long>> cachedIds = cacheService.getProductIdsFromCache(
                    CacheStrategy.COLD, null, pageable
            );
            assertThat(cachedIds).isEmpty();
        }
    }

    @Nested
    @DisplayName("캐시 히트/미스 시나리오")
    class CacheHitMissTest {

        @Test
        @DisplayName("캐시 미스 시 MV에서 조회하고 캐시에 저장한다")
        void should_query_mv_and_cache_on_cache_miss() {
            // Given: 상품 데이터 생성 (캐시 없음)
            ProductTestFixture.createBrandsAndProducts(brandRepository, productRepository, 1, 25);
            mvService.syncMaterializedView();

            Pageable pageable = PageRequest.of(0, 20);

            // When: 첫 조회 (캐시 미스)
            ProductSearchFilter filter = new ProductSearchFilter(null, null, pageable);
            Page<ProductInfo> result = productFacade.getProducts(filter);

            // Then: 결과 반환됨
            assertThat(result.getContent()).hasSize(20);

            // Then: 캐시에 저장됨
            Optional<List<Long>> cachedIds = cacheService.getProductIdsFromCache(
                    CacheStrategy.HOT, null, pageable
            );
            assertThat(cachedIds).isPresent();
        }

        @Test
        @DisplayName("캐시 히트 시 DB 조회 없이 캐시에서 반환한다")
        void should_return_from_cache_without_db_query_on_cache_hit() {
            // Given: 상품 데이터 생성 및 캐시 워밍업
            ProductTestFixture.createBrandsAndProducts(brandRepository, productRepository, 1, 25);
            mvService.syncMaterializedView();

            Pageable pageable = PageRequest.of(0, 20);
            ProductSearchFilter filter = new ProductSearchFilter(null, null, pageable);

            // 첫 조회로 캐시 생성
            productFacade.getProducts(filter);

            // When: 두 번째 조회 (캐시 히트)
            Page<ProductInfo> result = productFacade.getProducts(filter);

            // Then: 결과 반환됨
            assertThat(result.getContent()).hasSize(20);

            // TODO: DB 조회 횟수 검증 (쿼리 카운터 사용)
        }

        @Test
        @DisplayName("상품 상세 조회 시 캐시 미스면 MV에서 조회하고 캐시에 저장한다")
        void should_cache_product_detail_on_cache_miss() {
            // Given: 상품 생성
            ProductTestFixture.createBrandsAndProducts(brandRepository, productRepository, 1, 5);
            mvService.syncMaterializedView();
            Long productId = 1L;

            // When: 상품 상세 조회 (캐시 미스)
            ProductDetailInfo result = productFacade.getProductDetail(productId, null);

            // Then: 결과 반환됨
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(productId);

            // Then: 캐시에 저장됨
            Optional<ProductDetailInfo> cached = cacheService.getProductDetailFromCache(productId);
            assertThat(cached).isPresent();
            assertThat(cached.get().id()).isEqualTo(productId);
        }

        @Test
        @DisplayName("상품 상세 조회 시 캐시 히트면 캐시에서 반환한다")
        void should_return_product_detail_from_cache_on_cache_hit() {
            // Given: 상품 생성 및 캐시 워밍업
            ProductTestFixture.createBrandsAndProducts(brandRepository, productRepository, 1, 5);
            mvService.syncMaterializedView();
            Long productId = 1L;

            // 첫 조회로 캐시 생성
            productFacade.getProductDetail(productId, null);

            // When: 두 번째 조회 (캐시 히트)
            ProductDetailInfo result = productFacade.getProductDetail(productId, null);

            // Then: 결과 반환됨
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(productId);

            // TODO: DB 조회 횟수 검증
        }
    }

    @Nested
    @DisplayName("캐시 무효화")
    class CacheEvictionTest {

        @Test
        @DisplayName("상품 정보 변경 시 해당 상품의 캐시가 무효화된다")
        void should_evict_cache_when_product_changes() {
            // Given: 상품 생성 및 캐시 생성
            ProductTestFixture.createBrandsAndProducts(brandRepository, productRepository, 1, 5);
            mvService.syncMaterializedView();
            Long productId = 1L;

            productFacade.getProductDetail(productId, null);
            assertThat(cacheService.getProductDetailFromCache(productId)).isPresent();

            // When: 캐시 무효화
            cacheService.evictProductDetail(productId);

            // Then: 캐시에서 제거됨
            assertThat(cacheService.getProductDetailFromCache(productId)).isEmpty();
        }

        @Test
        @DisplayName("전략별 캐시 무효화가 정상 작동한다")
        void should_evict_cache_by_strategy() {
            // Given: 상품 데이터 생성 및 여러 페이지 캐시 생성
            ProductTestFixture.createBrandsAndProducts(brandRepository, productRepository, 1, 150);
            mvService.syncMaterializedView();

            // Hot, Warm, Cold 데이터 캐시 생성
            productFacade.getProducts(new ProductSearchFilter(null, null, PageRequest.of(0, 20))); // Hot
            productFacade.getProducts(new ProductSearchFilter(null, null, PageRequest.of(2, 20))); // Warm
            productFacade.getProducts(new ProductSearchFilter(null, null, PageRequest.of(5, 20))); // Cold 저장 안함

            // When: Hot 캐시만 무효화
            cacheService.evictProductIdsByStrategy(CacheStrategy.HOT);

            // Then: Hot 캐시만 제거됨
            assertThat(cacheService.getProductIdsFromCache(CacheStrategy.HOT, null, PageRequest.of(0, 20))).isEmpty();

            // Warm 은 유지 콜드는 저장 안함
            assertThat(cacheService.getProductIdsFromCache(CacheStrategy.WARM, null, PageRequest.of(2, 20))).isPresent();
            assertThat(cacheService.getProductIdsFromCache(CacheStrategy.COLD, null, PageRequest.of(5, 20))).isEmpty();
        }
    }
}
