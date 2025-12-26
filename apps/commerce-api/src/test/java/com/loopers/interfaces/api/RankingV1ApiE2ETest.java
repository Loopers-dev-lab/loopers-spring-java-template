package com.loopers.interfaces.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;

import com.loopers.cache.CacheKeyGenerator;
import com.loopers.cache.RankingRedisService;
import com.loopers.cache.dto.CachePayloads.RankingScore;
import com.loopers.cache.dto.CachePayloads.RankingScore.EventType;
import com.loopers.domain.brand.BrandEntity;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.product.*;
import com.loopers.fixtures.BrandTestFixture;
import com.loopers.fixtures.ProductTestFixture;
import com.loopers.interfaces.api.common.PageResponse;
import com.loopers.interfaces.api.product.ProductV1Dtos;
import com.loopers.support.Uris;
import com.loopers.utils.DatabaseCleanUp;
import com.loopers.utils.RedisCleanUp;

/**
 * 랭킹 API E2E 테스트
 * <p>
 * 실제 데이터 생성 → Redis ZSET 적재 → API 조회 전체 프로세스 검증
 *
 * @author hyunjikoh
 * @since 2025.12.26
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Ranking API E2E 테스트")
class RankingV1ApiE2ETest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private RedisCleanUp redisCleanUp;

    @Autowired
    private ProductService productService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private ProductMVService productMVService;

    @Autowired
    private RankingRedisService rankingRedisService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private CacheKeyGenerator cacheKeyGenerator;

    private Long testBrandId;
    private final List<Long> testProductIds = new ArrayList<>();
    private LocalDate today;

    @BeforeEach
    void setUp() {
        databaseCleanUp.truncateAllTables();
        redisCleanUp.truncateAll();
        testProductIds.clear();
        testBrandId = null;

        today = LocalDate.now();

        // 테스트용 브랜드 생성
        BrandEntity brand = brandService.registerBrand(
                BrandTestFixture.createRequest("랭킹테스트브랜드", "랭킹 E2E 테스트용 브랜드")
        );
        testBrandId = brand.getId();

        // 테스트용 상품 5개 생성
        for (int i = 1; i <= 5; i++) {
            ProductDomainCreateRequest productRequest = ProductTestFixture.createRequest(
                    testBrandId,
                    "랭킹테스트상품" + i,
                    "랭킹 E2E 테스트용 상품 " + i,
                    new BigDecimal(String.valueOf(10000 * i)),
                    new BigDecimal(String.valueOf(8000 * i)),
                    100
            );
            ProductEntity product = productService.registerProduct(productRequest);
            testProductIds.add(product.getId());
        }

        // MV 동기화
        productMVService.syncMaterializedView();
    }

    @Nested
    @DisplayName("랭킹 목록 조회 API")
    class GetRankingProductsTest {

        @Test
        @DisplayName("랭킹 데이터가 있으면 점수 순으로 상품 목록을 반환한다")
        void should_return_products_in_ranking_order() {
            // Given - Redis에 랭킹 데이터 직접 적재
            Long product1 = testProductIds.get(0); // 1위 (높은 점수)
            Long product2 = testProductIds.get(1); // 3위
            Long product3 = testProductIds.get(2); // 2위

            // 점수 적재 (높은 순: product1 > product2 > product3)
            List<RankingScore> scores = List.of(
                    new RankingScore(product1, EventType.PAYMENT_SUCCESS, 100.0, System.currentTimeMillis()),
                    new RankingScore(product2, EventType.LIKE_ACTION, 10.0, System.currentTimeMillis()),
                    new RankingScore(product3, EventType.PRODUCT_VIEW, 50.0, System.currentTimeMillis())
            );
            rankingRedisService.updateRankingScoresBatch(scores, today);

            // When
            ParameterizedTypeReference<ApiResponse<PageResponse<ProductV1Dtos.ProductListResponse>>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<PageResponse<ProductV1Dtos.ProductListResponse>>> response =
                    testRestTemplate.exchange(
                            Uris.Ranking.GET_RANKING + "?page=0&size=10",
                            HttpMethod.GET, null, responseType
                    );

            // Then
            assertAll(
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().content()).hasSize(3),
                    () -> assertThat(response.getBody().data().content().get(0).productId()).isEqualTo(product1),
                    () -> assertThat(response.getBody().data().content().get(2).productId()).isEqualTo(product2),
                    () -> assertThat(response.getBody().data().content().get(1).productId()).isEqualTo(product3)
            );
        }

        @Test
        @DisplayName("랭킹 데이터가 없으면 빈 목록을 반환한다")
        void should_return_empty_when_no_ranking_data() {
            // Given - 랭킹 데이터 없음

            // When
            ParameterizedTypeReference<ApiResponse<PageResponse<ProductV1Dtos.ProductListResponse>>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<PageResponse<ProductV1Dtos.ProductListResponse>>> response =
                    testRestTemplate.exchange(
                            Uris.Ranking.GET_RANKING,
                            HttpMethod.GET, null, responseType
                    );

            // Then
            assertAll(
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().content()).isEmpty(),
                    () -> assertThat(response.getBody().data().totalElements()).isEqualTo(0)
            );
        }

        @Test
        @DisplayName("페이징이 정상적으로 동작한다")
        void should_paginate_ranking_results() {
            // Given - 5개 상품 모두 랭킹에 등록
            List<RankingScore> scores = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                scores.add(new RankingScore(
                        testProductIds.get(i),
                        EventType.PRODUCT_VIEW,
                        (5 - i) * 10.0, // 점수: 50, 40, 30, 20, 10
                        System.currentTimeMillis()
                ));
            }
            rankingRedisService.updateRankingScoresBatch(scores, today);

            // When - 페이지 크기 2로 조회
            ParameterizedTypeReference<ApiResponse<PageResponse<ProductV1Dtos.ProductListResponse>>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<PageResponse<ProductV1Dtos.ProductListResponse>>> response =
                    testRestTemplate.exchange(
                            Uris.Ranking.GET_RANKING + "?page=0&size=2",
                            HttpMethod.GET, null, responseType
                    );

            // Then
            assertAll(
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().content()).hasSize(2),
                    () -> assertThat(response.getBody().data().totalElements()).isEqualTo(5),
                    () -> assertThat(response.getBody().data().totalPages()).isEqualTo(3),
                    () -> assertThat(response.getBody().data().first()).isTrue(),
                    () -> assertThat(response.getBody().data().last()).isFalse()
            );
        }

        @Test
        @DisplayName("특정 날짜의 랭킹을 조회할 수 있다")
        void should_return_ranking_for_specific_date() {
            // Given - 어제 날짜에 랭킹 데이터 적재
            LocalDate yesterday = today.minusDays(1);
            Long product1 = testProductIds.get(0);

            List<RankingScore> scores = List.of(
                    new RankingScore(product1, EventType.PRODUCT_VIEW, 100.0, System.currentTimeMillis())
            );
            rankingRedisService.updateRankingScoresBatch(scores, yesterday);

            // When - 어제 날짜로 조회
            ParameterizedTypeReference<ApiResponse<PageResponse<ProductV1Dtos.ProductListResponse>>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<PageResponse<ProductV1Dtos.ProductListResponse>>> response =
                    testRestTemplate.exchange(
                            Uris.Ranking.GET_RANKING + "?date=" + yesterday,
                            HttpMethod.GET, null, responseType
                    );

            // Then
            assertAll(
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().content()).hasSize(1),
                    () -> assertThat(response.getBody().data().content().get(0).productId()).isEqualTo(product1)
            );
        }
    }

    @Nested
    @DisplayName("콜드 스타트 Fallback 테스트")
    class ColdStartFallbackTest {

        @Test
        @DisplayName("오늘 랭킹이 없으면 어제 랭킹을 반환한다")
        void should_fallback_to_yesterday_when_today_is_empty() {
            // Given - 어제 랭킹만 있음
            LocalDate yesterday = today.minusDays(1);
            Long product1 = testProductIds.get(0);

            List<RankingScore> scores = List.of(
                    new RankingScore(product1, EventType.PRODUCT_VIEW, 50.0, System.currentTimeMillis())
            );
            rankingRedisService.updateRankingScoresBatch(scores, yesterday);

            // When - 날짜 미지정 (오늘 기준)
            ParameterizedTypeReference<ApiResponse<PageResponse<ProductV1Dtos.ProductListResponse>>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<PageResponse<ProductV1Dtos.ProductListResponse>>> response =
                    testRestTemplate.exchange(
                            Uris.Ranking.GET_RANKING,
                            HttpMethod.GET, null, responseType
                    );

            // Then - 어제 랭킹이 반환됨
            assertAll(
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().content()).hasSize(1),
                    () -> assertThat(response.getBody().data().content().get(0).productId()).isEqualTo(product1)
            );
        }

        @Test
        @DisplayName("명시적 날짜 지정 시 Fallback하지 않는다")
        void should_not_fallback_when_date_is_explicitly_specified() {
            // Given - 어제 랭킹만 있음
            LocalDate yesterday = today.minusDays(1);
            Long product1 = testProductIds.get(0);

            List<RankingScore> scores = List.of(
                    new RankingScore(product1, EventType.PRODUCT_VIEW, 50.0, System.currentTimeMillis())
            );
            rankingRedisService.updateRankingScoresBatch(scores, yesterday);

            // When - 오늘 날짜 명시적 지정
            ParameterizedTypeReference<ApiResponse<PageResponse<ProductV1Dtos.ProductListResponse>>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<PageResponse<ProductV1Dtos.ProductListResponse>>> response =
                    testRestTemplate.exchange(
                            Uris.Ranking.GET_RANKING + "?date=" + today,
                            HttpMethod.GET, null, responseType
                    );

            // Then - 빈 결과 (Fallback 안 함)
            assertAll(
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().content()).isEmpty()
            );
        }
    }

    @Nested
    @DisplayName("상품 상세 조회 시 랭킹 정보 포함 테스트")
    class ProductDetailWithRankingTest {

        @Test
        @DisplayName("랭킹에 있는 상품은 랭킹 정보가 포함된다")
        void should_include_ranking_info_for_ranked_product() {
            // Given - 상품을 랭킹에 등록
            Long productId = testProductIds.get(0);
            double score = 123.45;

            List<RankingScore> scores = List.of(
                    new RankingScore(productId, EventType.PAYMENT_SUCCESS, score, System.currentTimeMillis())
            );
            rankingRedisService.updateRankingScoresBatch(scores, today);

            // When
            ParameterizedTypeReference<ApiResponse<ProductV1Dtos.ProductDetailResponse>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<ProductV1Dtos.ProductDetailResponse>> response =
                    testRestTemplate.exchange(
                            Uris.Product.GET_DETAIL,
                            HttpMethod.GET, null, responseType, productId
                    );

            // Then
            assertAll(
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().productId()).isEqualTo(productId),
                    () -> assertThat(response.getBody().data().ranking()).isNotNull(),
                    () -> assertThat(response.getBody().data().ranking().rank()).isEqualTo(1L),
                    () -> assertThat(response.getBody().data().ranking().score()).isGreaterThan(0)
            );
        }

        @Test
        @DisplayName("랭킹에 없는 상품은 랭킹 정보가 null이다")
        void should_have_null_ranking_for_unranked_product() {
            // Given - 랭킹 데이터 없음
            Long productId = testProductIds.get(0);

            // When
            ParameterizedTypeReference<ApiResponse<ProductV1Dtos.ProductDetailResponse>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<ProductV1Dtos.ProductDetailResponse>> response =
                    testRestTemplate.exchange(
                            Uris.Product.GET_DETAIL,
                            HttpMethod.GET, null, responseType, productId
                    );

            // Then
            assertAll(
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().productId()).isEqualTo(productId),
                    () -> assertThat(response.getBody().data().ranking()).isNull()
            );
        }

        @Test
        @DisplayName("여러 상품 중 특정 상품의 순위가 정확히 반환된다")
        void should_return_correct_rank_among_multiple_products() {
            // Given - 3개 상품 랭킹 등록 (product2가 2위)
            Long product1 = testProductIds.get(0);
            Long product2 = testProductIds.get(1);
            Long product3 = testProductIds.get(2);

            List<RankingScore> scores = List.of(
                    new RankingScore(product1, EventType.PAYMENT_SUCCESS, 100.0, System.currentTimeMillis()),
                    new RankingScore(product2, EventType.LIKE_ACTION, 50.0, System.currentTimeMillis()),
                    new RankingScore(product3, EventType.PRODUCT_VIEW, 10.0, System.currentTimeMillis())
            );
            rankingRedisService.updateRankingScoresBatch(scores, today);

            // When - product2 상세 조회
            ParameterizedTypeReference<ApiResponse<ProductV1Dtos.ProductDetailResponse>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<ProductV1Dtos.ProductDetailResponse>> response =
                    testRestTemplate.exchange(
                            Uris.Product.GET_DETAIL,
                            HttpMethod.GET, null, responseType, product2
                    );

            // Then - 2위로 반환
            assertAll(
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().productId()).isEqualTo(product2),
                    () -> assertThat(response.getBody().data().ranking()).isNotNull(),
                    () -> assertThat(response.getBody().data().ranking().rank()).isEqualTo(2L)
            );
        }
    }

    @Nested
    @DisplayName("점수 누적 테스트")
    class ScoreAccumulationTest {

        @Test
        @DisplayName("동일 상품에 여러 이벤트 점수가 누적된다")
        void should_accumulate_scores_for_same_product() {
            // Given - 동일 상품에 여러 점수 적재
            Long productId = testProductIds.get(0);

            // 첫 번째 점수 적재 (PRODUCT_VIEW: weight 0.1, score 10.0 → 1.0)
            rankingRedisService.updateRankingScoresBatch(
                    List.of(new RankingScore(productId, EventType.PRODUCT_VIEW, 10.0, System.currentTimeMillis())),
                    today
            );

            // 두 번째 점수 적재 (LIKE_ACTION: weight 0.2, score 20.0 → 4.0)
            // 누적 점수: 1.0 + 4.0 = 5.0
            rankingRedisService.updateRankingScoresBatch(
                    List.of(new RankingScore(productId, EventType.LIKE_ACTION, 20.0, System.currentTimeMillis())),
                    today
            );

            // When
            ParameterizedTypeReference<ApiResponse<ProductV1Dtos.ProductDetailResponse>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<ProductV1Dtos.ProductDetailResponse>> response =
                    testRestTemplate.exchange(
                            Uris.Product.GET_DETAIL,
                            HttpMethod.GET, null, responseType, productId
                    );

            // Then - 점수가 누적됨 (weight 적용: 10*0.1 + 20*0.2 = 5.0)
            assertAll(
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().ranking()).isNotNull(),
                    () -> assertThat(response.getBody().data().ranking().score()).isGreaterThanOrEqualTo(5.0)
            );
        }
    }

    @Nested
    @DisplayName("Score Carry-Over 테스트")
    class CarryOverTest {

        @Test
        @DisplayName("Carry-Over 후 다음 날 랭킹에 점수가 이월된다")
        void should_carry_over_scores_to_next_day() {
            // Given - 오늘 랭킹 데이터 직접 Redis에 적재 (weight 적용된 점수)
            Long productId = testProductIds.get(0);
            double weightedScore = 60.0; // PAYMENT_SUCCESS weight 0.6 * score 100 = 60

            String todayKey = cacheKeyGenerator.generateDailyRankingKey(today);
            redisTemplate.opsForZSet().add(todayKey, productId.toString(), weightedScore);

            LocalDate tomorrow = today.plusDays(1);
            String tomorrowKey = cacheKeyGenerator.generateDailyRankingKey(tomorrow);
            redisTemplate.delete(tomorrowKey); // 내일 키 정리

            // When - Carry-Over 실행 (10%)
            rankingRedisService.carryOverScores(today, tomorrow, 0.1);

            // Then - 내일 키에 10% 점수가 이월됨
            Double tomorrowScore = redisTemplate.opsForZSet().score(tomorrowKey, productId.toString());

            assertThat(tomorrowScore).isNotNull();
            assertThat(tomorrowScore).isCloseTo(weightedScore * 0.1, org.assertj.core.data.Offset.offset(0.01));

            // Cleanup
            redisTemplate.delete(tomorrowKey);
        }
    }
}
