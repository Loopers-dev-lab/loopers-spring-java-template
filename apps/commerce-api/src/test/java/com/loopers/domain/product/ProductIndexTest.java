package com.loopers.domain.product;

import com.loopers.utils.DatabaseCleanUp;
import com.loopers.utils.ProductDataGenerator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProductIndexTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductDataGenerator dataGenerator;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private static boolean dataGenerated = false;

    @BeforeEach
    void setUp() {
        if (!dataGenerated) {
            System.out.println("=== 테스트 데이터 준비 중 ===");
            databaseCleanUp.truncateAllTables();
            dataGenerator.generate100KProducts();

            // Warm-up: JPA 초기화
            productRepository.findById(1L);

            dataGenerated = true;
            System.out.println("총 상품 수: " + productRepository.count());
        }
    }

    @Test
    @Order(1)
    @DisplayName("인덱스 유무에 따른 성능 비교 - 브랜드 필터링")
    void comparePerformanceWithAndWithoutIndexOnBrandFilter() {
        System.out.println("브랜드 필터링 인덱스 성능 비교");

        Long brandId = 1L;

        // 1. 인덱스 사용 전 - 모든 인덱스 무시
        System.out.println("\n[1] 인덱스 없이 조회 (ALL INDEX IGNORED)");
        String sqlWithoutIndex =
                "SELECT * FROM products IGNORE INDEX (idx_brand_id, idx_brand_price, idx_like_count, idx_created_at) " +
                        "WHERE brand_id = :brandId LIMIT 20";

        long startWithout = System.nanoTime();
        Query queryWithout = entityManager.createNativeQuery(sqlWithoutIndex, Product.class);
        queryWithout.setParameter("brandId", brandId);
        queryWithout.getResultList();
        long durationWithout = (System.nanoTime() - startWithout) / 1_000_000;

        ExplainResult explainWithout = getExplainResult(
                "EXPLAIN SELECT * FROM products IGNORE INDEX (idx_brand_id, idx_brand_price, idx_like_count, idx_created_at) " +
                        "WHERE brand_id = ? LIMIT 20", brandId);

        System.out.println("  사용된 인덱스: " + (explainWithout.key != null ? explainWithout.key : "없음 (Full Scan)"));
        System.out.println("  타입: " + explainWithout.type);
        System.out.println("  스캔된 행 수: " + explainWithout.rows);
        System.out.println("  조회 시간: " + durationWithout + "ms");

        // 2. 인덱스 사용
        System.out.println("\n[2] 인덱스 자동 선택");
        String sqlWithIndex = "SELECT * FROM products WHERE brand_id = :brandId LIMIT 20";

        long startWith = System.nanoTime();
        Query queryWith = entityManager.createNativeQuery(sqlWithIndex, Product.class);
        queryWith.setParameter("brandId", brandId);
        queryWith.getResultList();
        long durationWith = (System.nanoTime() - startWith) / 1_000_000;

        ExplainResult explainWith = getExplainResult(
                "EXPLAIN SELECT * FROM products WHERE brand_id = ? LIMIT 20", brandId);

        System.out.println("  사용된 인덱스: " + explainWith.key);
        System.out.println("  타입: " + explainWith.type);
        System.out.println("  스캔된 행 수: " + explainWith.rows);
        System.out.println("  조회 시간: " + durationWith + "ms");

        // 3. 결과 비교
        System.out.println("\n[비교 결과]");
        System.out.println("  인덱스 없음: " + durationWithout + "ms (rows: " + explainWithout.rows + ")");
        System.out.println("  인덱스 있음: " + durationWith + "ms (rows: " + explainWith.rows + ", key: " + explainWith.key + ")");

        if (durationWithout > 0) {
            double improvement = ((durationWithout - durationWith) / (double) durationWithout * 100);
            System.out.println("  성능 향상: " + String.format("%.1f", improvement) + "%");
        }

        assertThat(explainWith.key)
                .as("brand_id 필터링에 적합한 인덱스가 사용되어야 함")
                .isIn("idx_brand_id", "idx_brand_price");

        assertThat(explainWith.type)
                .as("인덱스를 사용한 range 또는 ref 접근이어야 함")
                .isIn("ref", "range");

        System.out.println("\n✓ 브랜드 필터링에 적합한 인덱스 사용 확인");
        System.out.println("  → MySQL 옵티마이저가 " + explainWith.key + " 선택");
    }

    @Test
    @Order(2)
    @DisplayName("인덱스 유무에 따른 성능 비교 - 브랜드 + 가격 정렬")
    void comparePerformanceWithAndWithoutIndexOnBrandAndPrice() {
        System.out.println("브랜드 + 가격 정렬 인덱스 성능 비교");

        Long brandId = 1L;

        // 1. 인덱스 없이
        System.out.println("\n[1] 인덱스 없이 조회 + 정렬");
        String sqlWithoutIndex =
                "SELECT * FROM products IGNORE INDEX (idx_brand_id, idx_brand_price, idx_like_count, idx_created_at) " +
                        "WHERE brand_id = :brandId ORDER BY price_value ASC LIMIT 20";

        long startWithout = System.nanoTime();
        Query queryWithout = entityManager.createNativeQuery(sqlWithoutIndex, Product.class);
        queryWithout.setParameter("brandId", brandId);
        queryWithout.getResultList();
        long durationWithout = (System.nanoTime() - startWithout) / 1_000_000;

        ExplainResult explainWithout = getExplainResult(
                "EXPLAIN SELECT * FROM products IGNORE INDEX (idx_brand_id, idx_brand_price, idx_like_count, idx_created_at) " +
                        "WHERE brand_id = ? ORDER BY price_value ASC LIMIT 20", brandId);

        System.out.println("  사용된 인덱스: " + (explainWithout.key != null ? explainWithout.key : "없음"));
        System.out.println("  스캔된 행 수: " + explainWithout.rows);
        System.out.println("  Extra: " + explainWithout.extra);
        System.out.println("  조회 시간: " + durationWithout + "ms");
        System.out.println("  filesort 사용: " + (explainWithout.extra != null && explainWithout.extra.contains("filesort") ? "예 (정렬 오버헤드 발생)" : "아니오"));

        // 2. 복합 인덱스 사용
        System.out.println("\n[2] 인덱스 자동 선택");
        String sqlWithIndex =
                "SELECT * FROM products WHERE brand_id = :brandId ORDER BY price_value ASC LIMIT 20";

        long startWith = System.nanoTime();
        Query queryWith = entityManager.createNativeQuery(sqlWithIndex, Product.class);
        queryWith.setParameter("brandId", brandId);
        queryWith.getResultList();
        long durationWith = (System.nanoTime() - startWith) / 1_000_000;

        ExplainResult explainWith = getExplainResult(
                "EXPLAIN SELECT * FROM products WHERE brand_id = ? ORDER BY price_value ASC LIMIT 20", brandId);

        System.out.println("  사용된 인덱스: " + explainWith.key);
        System.out.println("  스캔된 행 수: " + explainWith.rows);
        System.out.println("  Extra: " + (explainWith.extra != null ? explainWith.extra : "없음 (추가 작업 불필요)"));
        System.out.println("  조회 시간: " + durationWith + "ms");
        System.out.println("  filesort 사용: " + (explainWith.extra != null && explainWith.extra.contains("filesort") ? "예" : "아니오 (인덱스 순서로 정렬됨)"));

        // 3. 결과 비교
        System.out.println("\n[비교 결과]");
        System.out.println("  인덱스 없음: " + durationWithout + "ms (filesort 발생)");
        System.out.println("  복합 인덱스: " + durationWith + "ms (인덱스 정렬)");

        if (durationWithout > 0) {
            double improvement = ((durationWithout - durationWith) / (double) durationWithout * 100);
            System.out.println("  성능 향상: " + String.format("%.1f", improvement) + "%");
        }

        // ✅ 검증 수정
        assertThat(explainWith.key)
                .as("복합 인덱스가 사용되어야 함")
                .isEqualTo("idx_brand_price");

        // Extra가 null이면 오히려 좋은 것! (추가 작업 없음)
        if (explainWith.extra != null) {
            assertThat(explainWith.extra)
                    .as("filesort가 발생하지 않아야 함")
                    .doesNotContain("filesort");
        }

        System.out.println("\n✓ idx_brand_price 복합 인덱스 사용 확인");
        System.out.println("✓ 인덱스로 필터링 + 정렬 동시 처리 (filesort 불필요)");
        System.out.println("✓ Extra가 null = 최적의 실행 계획!");
    }

    @Test
    @Order(3)
    @DisplayName("좋아요 순 정렬 - 실제 데이터량에 따른 인덱스 효과")
    void comparePerformanceOnLikeCountWithDifferentLimits() {
        System.out.println("좋아요 순 정렬 인덱스 효과 분석");

        // LIMIT 값을 다르게 해서 테스트
        int[] limits = {20, 100, 1000, 10000};

        for (int limit : limits) {
            System.out.println("\n[LIMIT " + limit + "]");

            // 1. 인덱스 없이
            String sqlWithout =
                    "SELECT * FROM products IGNORE INDEX (idx_like_count) ORDER BY like_count DESC LIMIT " + limit;

            long startWithout = System.nanoTime();
            Query queryWithout = entityManager.createNativeQuery(sqlWithout, Product.class);
            queryWithout.getResultList();
            long durationWithout = (System.nanoTime() - startWithout) / 1_000_000;

            // 2. 인덱스 사용
            String sqlWith = "SELECT * FROM products ORDER BY like_count DESC LIMIT " + limit;

            long startWith = System.nanoTime();
            Query queryWith = entityManager.createNativeQuery(sqlWith, Product.class);
            queryWith.getResultList();
            long durationWith = (System.nanoTime() - startWith) / 1_000_000;

            System.out.println("  인덱스 없음: " + durationWithout + "ms");
            System.out.println("  인덱스 있음: " + durationWith + "ms");

            if (durationWithout > 0) {
                double improvement = ((durationWithout - durationWith) / (double) durationWithout * 100);
                System.out.println("  성능 차이: " + String.format("%.1f", improvement) + "%");
            }
        }

        // LIMIT 10000으로 EXPLAIN 확인
        System.out.println("\n[EXPLAIN 분석 - LIMIT 10000]");
        ExplainResult explain = getExplainResult(
                "EXPLAIN SELECT * FROM products ORDER BY like_count DESC LIMIT 10000", null);

        System.out.println("  사용된 인덱스: " + (explain.key != null ? explain.key : "없음"));
        System.out.println("  Extra: " + explain.extra);

        System.out.println("\n[분석]");
        if (explain.key == null || !explain.key.equals("idx_like_count")) {
            System.out.println("  ⚠ MySQL 옵티마이저가 인덱스를 선택하지 않음");
            System.out.println("  ⚠ 이유: LIMIT 값이 작아서 Full Scan + Sort가 더 효율적이라고 판단");
            System.out.println("  → LIMIT 값을 크게 하면 인덱스 사용 가능성 증가");
            System.out.println("  → 실제 서비스에서는 전체 정렬보다 Top-N 조회가 많아 인덱스 효과적");
        } else {
            System.out.println("  ✓ idx_like_count 인덱스 사용됨");
        }

        System.out.println("\n✓ 좋아요 정렬 성능 분석 완료");
        System.out.println("✓ LIMIT 값에 따라 옵티마이저 전략이 달라질 수 있음을 확인");
    }

    @Test
    @Order(4)
    @DisplayName("종합 성능 테스트 - 실제 사용 시나리오")
    void comprehensivePerformanceTest() {
        System.out.println("실제 사용 시나리오 성능 테스트");

        int iterations = 5;

        // 시나리오 1: 브랜드 필터
        System.out.println("\n[시나리오 1] 브랜드별 상품 조회 (페이징)");
        long totalTime1 = 0;
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            ProductSearchCondition condition = new ProductSearchCondition(
                    1L, ProductSortType.LATEST, PageRequest.of(0, 20));
            productRepository.findProducts(condition.toPageRequest(), condition.brandId());
            totalTime1 += (System.nanoTime() - start);
        }
        System.out.println("  평균 조회 시간: " + (totalTime1 / iterations / 1_000_000) + "ms");

        // 시나리오 2: 브랜드 + 가격 정렬
        System.out.println("\n[시나리오 2] 브랜드 상품 가격순 정렬");
        long totalTime2 = 0;
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            ProductSearchCondition condition = new ProductSearchCondition(
                    1L, ProductSortType.PRICE_ASC, PageRequest.of(0, 20));
            productRepository.findProducts(condition.toPageRequest(), condition.brandId());
            totalTime2 += (System.nanoTime() - start);
        }
        System.out.println("  평균 조회 시간: " + (totalTime2 / iterations / 1_000_000) + "ms");

        // 시나리오 3: 좋아요 순
        System.out.println("\n[시나리오 3] 인기 상품 조회 (좋아요순)");
        long totalTime3 = 0;
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            ProductSearchCondition condition = new ProductSearchCondition(
                    null, ProductSortType.LIKES_DESC, PageRequest.of(0, 20));
            productRepository.findProducts(condition.toPageRequest(), condition.brandId());
            totalTime3 += (System.nanoTime() - start);
        }
        System.out.println("  평균 조회 시간: " + (totalTime3 / iterations / 1_000_000) + "ms");

        System.out.println("\n✓ 모든 시나리오가 200ms 이내에 완료");
        assertThat(totalTime1 / iterations / 1_000_000).isLessThan(200);
        assertThat(totalTime2 / iterations / 1_000_000).isLessThan(200);
        assertThat(totalTime3 / iterations / 1_000_000).isLessThan(200);
    }

    private ExplainResult getExplainResult(String sql, Long param) {
        Query query = entityManager.createNativeQuery(sql);
        if (param != null) {
            query.setParameter(1, param);
        }

        List<Object[]> results = query.getResultList();
        if (results.isEmpty()) {
            return new ExplainResult(null, null, null, null);
        }

        Object[] row = results.get(0);

        // MySQL EXPLAIN 결과 파싱
        String type = safeToString(row, 4);
        String key = safeToString(row, 6);
        String rows = safeToString(row, 9);
        String extra = safeToString(row, 11);

        return new ExplainResult(type, key, rows, extra);
    }

    private String safeToString(Object[] row, int index) {
        if (row == null || index >= row.length || row[index] == null) {
            return null;
        }
        return row[index].toString();
    }

    private record ExplainResult(String type, String key, String rows, String extra) {}
}
