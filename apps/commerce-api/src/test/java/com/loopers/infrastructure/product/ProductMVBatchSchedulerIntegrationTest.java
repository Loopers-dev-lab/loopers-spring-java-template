//package com.loopers.infrastructure.product;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.awaitility.Awaitility.await;
//
//import java.math.BigDecimal;
//import java.time.Duration;
//
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.TestPropertySource;
//
//import com.loopers.domain.product.ProductMVService;
//import com.loopers.domain.brand.BrandEntity;
//import com.loopers.domain.brand.BrandRepository;
//import com.loopers.domain.product.ProductEntity;
//import com.loopers.domain.product.ProductMVRepository;
//import com.loopers.domain.product.ProductRepository;
//import com.loopers.fixtures.BrandTestFixture;
//import com.loopers.fixtures.ProductTestFixture;
//import com.loopers.utils.DatabaseCleanUp;
//import com.loopers.utils.RedisCleanUp;
//
/// **
// * ProductMVBatchScheduler 통합 테스트
// *
// * @author hyunjikoh
// * @since 2025. 11. 10.
// */
//@SpringBootTest
//@TestPropertySource(properties = {
//    "spring.task.scheduling.pool.size=2"
//})
//@DisplayName("ProductMVBatchScheduler 통합 테스트")
//public class ProductMVBatchSchedulerIntegrationTest {
//
//    @Autowired
//    private DatabaseCleanUp databaseCleanUp;
//
//    @Autowired
//    private RedisCleanUp redisCleanUp;
//
//    @Autowired
//    private ProductMVBatchScheduler batchScheduler;
//
//    @Autowired
//    private ProductMVService mvService;
//
//    @Autowired
//    private BrandRepository brandRepository;
//
//    @Autowired
//    private ProductRepository productRepository;
//
//    @Autowired
//    private ProductMVRepository mvRepository;
//
//    @AfterEach
//    void tearDown() {
//        databaseCleanUp.truncateAllTables();
//        redisCleanUp.truncateAll();
//    }
//
//    @Nested
//    @DisplayName("MV 동기화 배치")
//    class SyncMaterializedViewTest {
//
//        @Test
//        @DisplayName("배치 실행 시 MV 테이블이 동기화된다")
//        void should_sync_mv_table_when_batch_runs() {
//            // Given: 상품 생성
//            BrandEntity brand = BrandTestFixture.createAndSave(brandRepository, "Test Brand", "Description");
//            ProductEntity product = ProductTestFixture.createAndSave(
//                productRepository,
//                brand,
//                "Test Product",
//                "Product Description",
//                new BigDecimal("10000"),
//                100
//            );
//
//            // When: 배치 실행
//            batchScheduler.syncMaterializedView();
//
//            // Then: MV 테이블에 데이터가 동기화됨
//            var mvEntity = mvRepository.findById(product.getId());
//            assertThat(mvEntity).isPresent();
//            assertThat(mvEntity.get().getName()).isEqualTo("Test Product");
//            assertThat(mvEntity.get().getBrandName()).isEqualTo("Test Brand");
//        }
//
//        @Test
//        @DisplayName("상품 정보 변경 후 배치 실행 시 MV가 갱신된다")
//        void should_update_mv_when_product_changes() {
//            // Given: 상품 생성 및 초기 동기화
//            BrandEntity brand = BrandTestFixture.createAndSave(brandRepository, "Test Brand", "Description");
//            ProductEntity product = ProductTestFixture.createAndSave(
//                productRepository,
//                brand,
//                "Original Name",
//                "Product Description",
//                new BigDecimal("10000"),
//                100
//            );
//            batchScheduler.syncMaterializedView();
//
//            // When: 상품 정보 변경
//            ProductEntity updatedProduct = new ProductEntity(
//                product.getBrandId(),
//                "Updated Name",
//                product.getDescription(),
//                product.getPrice().getOriginPrice(),
//                product.getPrice().getDiscountPrice(),
//                product.getStockQuantity()
//            );
//            productRepository.save(updatedProduct);
//
//            // When: 배치 재실행
//            batchScheduler.syncMaterializedView();
//
//            // Then: MV가 갱신됨
//            var mvEntity = mvRepository.findById(product.getId());
//            assertThat(mvEntity).isPresent();
//            assertThat(mvEntity.get().getName()).isEqualTo("Updated Name");
//        }
//
//        @Test
//        @DisplayName("브랜드 정보 변경 후 배치 실행 시 해당 브랜드의 모든 상품 MV가 갱신된다")
//        void should_update_all_products_mv_when_brand_changes() {
//            // Given: 브랜드와 여러 상품 생성
//            BrandEntity brand = BrandTestFixture.createAndSave(brandRepository, "Original Brand", "Description");
//            ProductEntity product1 = ProductTestFixture.createAndSave(productRepository, brand);
//            ProductEntity product2 = ProductTestFixture.createAndSave(productRepository, brand);
//            batchScheduler.syncMaterializedView();
//
//            // When: 브랜드 이름 변경
//            BrandEntity updatedBrand = new BrandEntity("Updated Brand", brand.getDescription());
//            brandRepository.save(updatedBrand);
//
//            // When: 배치 재실행
//            batchScheduler.syncMaterializedView();
//
//            // Then: 모든 상품의 MV가 갱신됨
//            var mvEntity1 = mvRepository.findById(product1.getId());
//            var mvEntity2 = mvRepository.findById(product2.getId());
//
//            assertThat(mvEntity1).isPresent();
//            assertThat(mvEntity1.get().getBrandName()).isEqualTo("Updated Brand");
//
//            assertThat(mvEntity2).isPresent();
//            assertThat(mvEntity2.get().getBrandName()).isEqualTo("Updated Brand");
//        }
//
//        @Test
//        @DisplayName("배치 실행 시 변경되지 않은 데이터는 업데이트하지 않는다")
//        void should_not_update_unchanged_data() {
//            // Given: 상품 생성 및 초기 동기화
//            BrandEntity brand = BrandTestFixture.createAndSave(brandRepository, "Test Brand", "Description");
//            ProductEntity product = ProductTestFixture.createAndSave(productRepository, brand);
//            batchScheduler.syncMaterializedView();
//
//            var mvBefore = mvRepository.findById(product.getId()).orElseThrow();
//            var lastUpdatedBefore = mvBefore.getLastUpdatedAt();
//
//            // When: 변경 없이 배치 재실행
//            batchScheduler.syncMaterializedView();
//
//            // Then: lastUpdatedAt이 변경되지 않음 (업데이트되지 않음)
//            var mvAfter = mvRepository.findById(product.getId()).orElseThrow();
//            assertThat(mvAfter.getLastUpdatedAt()).isEqualTo(lastUpdatedBefore);
//        }
//    }
//
//    @Nested
//    @DisplayName("Hot 캐시 갱신 배치")
//    class RefreshHotCacheTest {
//
//        @Test
//        @DisplayName("Hot 캐시 갱신 배치가 정상적으로 실행된다")
//        void should_refresh_hot_cache_successfully() {
//            // Given: 상품 데이터 준비
//            ProductTestFixture.createBrandsAndProducts(brandRepository, productRepository, 2, 5);
//            batchScheduler.syncMaterializedView();
//
//            // When: Hot 캐시 갱신 실행
//            batchScheduler.refreshHotCache();
//
//            // Then: 예외 없이 실행 완료 (로그 확인)
//            // TODO: 캐시 갱신 로직 구현 후 캐시 데이터 검증 추가
//        }
//    }
//
//    @Nested
//    @DisplayName("스케줄러 자동 실행")
//    class SchedulerAutoRunTest {
//
//        @Test
//        @DisplayName("스케줄러가 2분 간격으로 자동 실행된다")
//        void should_run_scheduler_automatically_every_2_minutes() {
//            // Given: 상품 생성
//            BrandEntity brand = BrandTestFixture.createAndSave(brandRepository, "Test Brand", "Description");
//            ProductEntity product = ProductTestFixture.createAndSave(productRepository, brand);
//
//            // When: 스케줄러가 자동으로 실행될 때까지 대기 (최대 3분)
//            await()
//                .atMost(Duration.ofMinutes(3))
//                .pollInterval(Duration.ofSeconds(10))
//                .untilAsserted(() -> {
//                    // Then: MV 테이블에 데이터가 동기화됨
//                    var mvEntity = mvRepository.findById(product.getId());
//                    assertThat(mvEntity).isPresent();
//                });
//        }
//    }
//}
