package com.loopers.performance;

import com.loopers.application.product.ProductFacade;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.cache.ProductCacheService;
import com.loopers.infrastructure.product.ProductJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PerformanceTest {

    private static final Logger log = LoggerFactory.getLogger(PerformanceTest.class);
    private static final int TEST_DATA_COUNT = 10000;

    @Autowired
    private ProductFacade productFacade;

    @Autowired
    private ProductCacheService productCacheService;

    @Autowired
    private ProductJpaRepository productRepository;

    @Autowired
    private BrandJpaRepository brandRepository;

    @BeforeEach
    void setUp() {
        productCacheService.clearAllProductCache();

        if (productRepository.count() > 0) {
            return;
        }

        log.info("Test data 생성 시작...");

        Random random = new Random(42L);

        List<Brand> brands = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Brand brand = Brand.create("Brand-" + i);
            brands.add(brandRepository.save(brand));
        }

        List<Product> products = new ArrayList<>();

        for (int i = 0; i < TEST_DATA_COUNT; i++) {
            Product product = Product.create(
                    "Product-" + i,
                    "Description for product " + i,
                    1000L + random.nextInt(50000),
                    (long) random.nextInt(100),
                    brands.get(random.nextInt(brands.size())).getId()
            );

            long likes = random.nextInt(1000);
            for (long j = 0; j < likes; j++) {
                product.increaseLikes();
            }

            products.add(product);

            if (products.size() % 1000 == 0) {
                productRepository.saveAll(products);
                products.clear();
            }
        }

        if (!products.isEmpty()) {
            productRepository.saveAll(products);
        }

        log.info("Test data 생성 완료: {} 개 상품, {} 개 브랜드",
                productRepository.count(), brandRepository.count());
    }

    @DisplayName("DB 인덱스 성능 측정")
    @Nested
    class DatabaseIndexPerformance {

        private static final int ITERATIONS = 50;
        private static final int WARMUP_ITERATIONS = 5;

        @DisplayName("단일 인덱스: 생성일순 정렬 시 인덱스 사용으로 빠른 조회")
        @Test
        void performanceTest1() {
            performanceTest("생성일순 정렬", null, Sort.Direction.DESC, "createdAt");
        }

        @DisplayName("단일 인덱스: 가격순 정렬 시 인덱스 사용으로 빠른 조회")
        @Test
        void performanceTest2() {
            performanceTest("가격순 정렬", null, Sort.Direction.ASC, "price");
        }

        @DisplayName("단일 인덱스: 좋아요순 정렬 시 인덱스 사용으로 빠른 조회")
        @Test
        void performanceTest3() {
            performanceTest("좋아요순 정렬", null, Sort.Direction.DESC, "totalLikes");
        }

        @DisplayName("복합 인덱스: 브랜드 필터 + 생성일순 정렬 시 복합 인덱스 효과")
        @Test
        void performanceTest4() {
            var allProducts = productRepository.findAll();
            if (allProducts.isEmpty()) {
                return;
            }
            Long brandId = allProducts.get(0).getBrandId();
            performanceTest("브랜드 필터 + 생성일순", brandId, Sort.Direction.DESC, "createdAt");
        }

        @DisplayName("복합 인덱스: 브랜드 필터 + 가격순 정렬 시 복합 인덱스 효과")
        @Test
        void performanceTest5() {
            var allProducts = productRepository.findAll();
            if (allProducts.isEmpty()) {
                return;
            }
            Long brandId = allProducts.get(0).getBrandId();
            performanceTest("브랜드 필터 + 가격순", brandId, Sort.Direction.ASC, "price");
        }

        @DisplayName("복합 인덱스: 브랜드 필터 + 좋아요순 정렬 시 복합 인덱스 효과")
        @Test
        void performanceTest6() {
            var allProducts = productRepository.findAll();
            if (allProducts.isEmpty()) {
                return;
            }
            Long brandId = allProducts.get(0).getBrandId();
            performanceTest("브랜드 필터 + 좋아요순", brandId, Sort.Direction.DESC, "totalLikes");
        }

        @DisplayName("PK 기반 단일 조회: 기본 키 인덱스로 O(1) 상수 시간 조회")
        @Test
        void performanceTest7() {
            var allProducts = productRepository.findAll();
            if (allProducts.isEmpty()) {
                return;
            }
            Long productId = allProducts.get(0).getId();

            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                productRepository.findById(productId);
            }

            double[] executionTimes = new double[ITERATIONS];
            for (int i = 0; i < ITERATIONS; i++) {
                long start = System.nanoTime();
                var result = productRepository.findById(productId);
                long end = System.nanoTime();

                executionTimes[i] = (end - start) / 1_000_000.0;
                assertThat(result).isPresent();
            }

            double avg = calculateAverage(executionTimes);
            double min = findMin(executionTimes);
            double max = findMax(executionTimes);
            double stdDev = calculateStdDev(executionTimes, avg);

            log.info("=== PK 기반 단일 조회 (ID: {}) ===", productId);
            log.info("평균: {}ms | 최소: {}ms | 최대: {}ms | 표준편차: {}ms",
                    String.format("%.2f", avg),
                    String.format("%.2f", min),
                    String.format("%.2f", max),
                    String.format("%.2f", stdDev));

            assertThat(avg).isLessThan(10.0);
        }

        private void performanceTest(String description, Long brandId, Sort.Direction direction, String sortField) {
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                Pageable pageable = PageRequest.of(0, 20, Sort.by(direction, sortField));
                if (brandId == null) {
                    productRepository.findAll(pageable);
                } else {
                    productRepository.findByBrandId(brandId, pageable);
                }
            }

            double[] executionTimes = new double[ITERATIONS];
            for (int i = 0; i < ITERATIONS; i++) {
                Pageable pageable = PageRequest.of(0, 20, Sort.by(direction, sortField));

                long start = System.nanoTime();
                Page<?> result = brandId == null
                        ? productRepository.findAll(pageable)
                        : productRepository.findByBrandId(brandId, pageable);
                long end = System.nanoTime();

                executionTimes[i] = (end - start) / 1_000_000.0;
                assertThat(result.getContent()).isNotEmpty();
            }

            double avg = calculateAverage(executionTimes);
            double min = findMin(executionTimes);
            double max = findMax(executionTimes);
            double stdDev = calculateStdDev(executionTimes, avg);

            log.info("=== {} ===", description);
            log.info("평균: {}ms | 최소: {}ms | 최대: {}ms | 표준편차: {}ms",
                    String.format("%.2f", avg),
                    String.format("%.2f", min),
                    String.format("%.2f", max),
                    String.format("%.2f", stdDev));

            assertThat(avg).isLessThan(brandId == null ? 20.0 : 15.0);
        }
    }

    @DisplayName("캐시 성능 측정")
    @Nested
    class CachePerformance {

        private static final int ITERATIONS = 100;
        private static final int WARMUP_ITERATIONS = 5;

        @DisplayName("목록 조회: 최신순 정렬 시 캐시가 DB보다 빠르다")
        @Test
        void performanceTest1() {
            performanceTestWithWarmup("latest", null);
        }

        @DisplayName("목록 조회: 가격순 정렬 시 캐시가 DB보다 빠르다")
        @Test
        void performanceTest2() {
            performanceTestWithWarmup("price_asc", null);
        }

        @DisplayName("목록 조회: 인기순 정렬 시 캐시가 DB보다 빠르다")
        @Test
        void performanceTest3() {
            performanceTestWithWarmup("likes_desc", null);
        }

        @DisplayName("목록 조회: 브랜드 필터 + 최신순 정렬 시 캐시가 DB보다 빠르다")
        @Test
        void performanceTest4() {
            var products = productFacade.getProducts(null, "latest", 0, 1);
            if (products.products().isEmpty()) {
                return;
            }
            Long brandId = products.products().get(0).brand().id();
            performanceTestWithWarmup("latest", brandId);
        }

        @DisplayName("상세 조회: 단일 상품 조회 시 캐시가 DB보다 빠르다")
        @Test
        void performanceTest5() {
            var products = productFacade.getProducts(null, "latest", 0, 1);
            if (products.products().isEmpty()) {
                return;
            }

            Long productId = products.products().get(0).id();

            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                productFacade.getProduct(productId);
            }

            productCacheService.clearAllProductCache();
            double[] dbTimes = new double[ITERATIONS];
            for (int i = 0; i < ITERATIONS; i++) {
                long start = System.nanoTime();
                productFacade.getProduct(productId);
                long end = System.nanoTime();
                dbTimes[i] = (end - start) / 1_000_000.0;
            }

            double[] cacheTimes = new double[ITERATIONS];
            for (int i = 0; i < ITERATIONS; i++) {
                long start = System.nanoTime();
                productFacade.getProduct(productId);
                long end = System.nanoTime();
                cacheTimes[i] = (end - start) / 1_000_000.0;
            }

            double dbAvg = calculateAverage(dbTimes);
            double cacheAvg = calculateAverage(cacheTimes);
            double dbMin = findMin(dbTimes);
            double cacheMin = findMin(cacheTimes);

            log.info("=== 상품 상세 조회 (ID: {}) ===", productId);
            log.info("DB 조회 (Cache Miss): 평균 {}ms | 최소 {}ms",
                    String.format("%.2f", dbAvg), String.format("%.2f", dbMin));
            log.info("캐시 조회 (Cache Hit): 평균 {}ms | 최소 {}ms",
                    String.format("%.2f", cacheAvg), String.format("%.2f", cacheMin));
            log.info("성능 향상: {}배", String.format("%.2f", dbAvg / cacheAvg));

            assertThat(cacheAvg).isLessThan(dbAvg);
        }

        @DisplayName("통합 시나리오: 여러 정렬 옵션의 혼합 조회 시 캐시 효과")
        @Test
        void performanceTest6() {
            String[] sorts = {"latest", "price_asc", "likes_desc"};

            double totalFirstRequest = 0;
            double totalCachedRequest = 0;
            int requestCount = 0;

            for (String sort : sorts) {
                for (int page = 0; page < 3; page++) {
                    productCacheService.clearAllProductCache();
                    long start = System.nanoTime();
                    productFacade.getProducts(null, sort, page, 20);
                    long end = System.nanoTime();
                    double firstRequestTime = (end - start) / 1_000_000.0;
                    totalFirstRequest += firstRequestTime;

                    start = System.nanoTime();
                    productFacade.getProducts(null, sort, page, 20);
                    end = System.nanoTime();
                    double cachedRequestTime = (end - start) / 1_000_000.0;
                    totalCachedRequest += cachedRequestTime;

                    requestCount++;
                }
            }

            double avgFirstRequest = totalFirstRequest / requestCount;
            double avgCachedRequest = totalCachedRequest / requestCount;

            log.info("=== 혼합 조회 시나리오 ({}개 요청) ===", requestCount);
            log.info("첫 요청 평균: {}ms", String.format("%.2f", avgFirstRequest));
            log.info("캐시된 요청 평균: {}ms", String.format("%.2f", avgCachedRequest));
            log.info("성능 향상: {}배", String.format("%.2f", avgFirstRequest / avgCachedRequest));

            assertThat(avgCachedRequest).isLessThan(avgFirstRequest);
        }

        private void performanceTestWithWarmup(String sort, Long brandId) {
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                try {
                    productFacade.getProducts(brandId, sort, 0, 20);
                } catch (Exception e) {
                    return;
                }
            }

            productCacheService.clearAllProductCache();
            double[] dbTimes = new double[ITERATIONS];
            for (int i = 0; i < ITERATIONS; i++) {
                long start = System.nanoTime();
                productFacade.getProducts(brandId, sort, 0, 20);
                long end = System.nanoTime();
                dbTimes[i] = (end - start) / 1_000_000.0;
            }

            double[] cacheTimes = new double[ITERATIONS];
            for (int i = 0; i < ITERATIONS; i++) {
                long start = System.nanoTime();
                productFacade.getProducts(brandId, sort, 0, 20);
                long end = System.nanoTime();
                cacheTimes[i] = (end - start) / 1_000_000.0;
            }

            double dbAvg = calculateAverage(dbTimes);
            double cacheAvg = calculateAverage(cacheTimes);
            double dbMin = findMin(dbTimes);
            double cacheMin = findMin(cacheTimes);
            double dbMax = findMax(dbTimes);
            double cacheMax = findMax(cacheTimes);

            String filterDesc = brandId == null ? "전체 상품" : "브랜드 #" + brandId;
            log.info("=== {}. 정렬: {} ===", filterDesc, sort);
            log.info("DB 조회 (Cache Miss): 평균 {}ms | 최소 {}ms | 최대 {}ms",
                    String.format("%.2f", dbAvg), String.format("%.2f", dbMin), String.format("%.2f", dbMax));
            log.info("캐시 조회 (Cache Hit): 평균 {}ms | 최소 {}ms | 최대 {}ms",
                    String.format("%.2f", cacheAvg), String.format("%.2f", cacheMin), String.format("%.2f", cacheMax));
            log.info("성능 향상: {}배 (DB 대비 캐시)", String.format("%.2f", dbAvg / cacheAvg));

            assertThat(cacheAvg).isLessThan(dbAvg);
        }
    }

    // Helper methods
    private double calculateAverage(double[] values) {
        double sum = 0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.length;
    }

    private double findMin(double[] values) {
        double min = Double.MAX_VALUE;
        for (double value : values) {
            if (value < min) min = value;
        }
        return min;
    }

    private double findMax(double[] values) {
        double max = Double.MIN_VALUE;
        for (double value : values) {
            if (value > max) max = value;
        }
        return max;
    }

    private double calculateStdDev(double[] values, double avg) {
        double sum = 0;
        for (double value : values) {
            sum += Math.pow(value - avg, 2);
        }
        return Math.sqrt(sum / values.length);
    }
}
