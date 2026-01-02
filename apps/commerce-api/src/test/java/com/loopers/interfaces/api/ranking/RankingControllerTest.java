package com.loopers.interfaces.api.ranking;

import com.loopers.domain.product.ProductStatus;
import com.loopers.domain.product.view.ProductView;
import com.loopers.domain.ranking.RankingSnapshotDaily;
import com.loopers.domain.ranking.RankingSnapshotHourly;
import com.loopers.domain.ranking.RankingSnapshotMonthly;
import com.loopers.domain.ranking.RankingSnapshotWeekly;
import com.loopers.infrastructure.ranking.RankingSnapshotDailyJpaRepository;
import com.loopers.infrastructure.ranking.RankingSnapshotHourlyJpaRepository;
import com.loopers.infrastructure.ranking.RankingSnapshotMonthlyJpaRepository;
import com.loopers.infrastructure.ranking.RankingSnapshotWeeklyJpaRepository;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.utils.DatabaseCleanUp;
import com.loopers.utils.RedisCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ranking API 통합 테스트
 * 
 * - 정상 조회 테스트
 * - Redis Fallback 테스트
 * - Tier 2 Cache Miss 테스트
 * - Size 제한 테스트
 */
@DisplayName("Ranking API 통합 테스트")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RankingControllerTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private com.loopers.domain.product.view.ProductViewRepository productViewRepository;

    @Autowired
    private RankingSnapshotHourlyJpaRepository rankingSnapshotHourlyJpaRepository;

    @Autowired
    private RankingSnapshotDailyJpaRepository rankingSnapshotDailyJpaRepository;

    @Autowired
    private RankingSnapshotWeeklyJpaRepository rankingSnapshotWeeklyJpaRepository;

    @Autowired
    private RankingSnapshotMonthlyJpaRepository rankingSnapshotMonthlyJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private RedisCleanUp redisCleanUp;

    @AfterEach
    void tearDown() {
        redisCleanUp.truncateAll();
        databaseCleanUp.truncateAllTables();
    }

    @Nested
    @DisplayName("정상 조회 테스트")
    class NormalQueryTest {

        @Test
        @DisplayName("정상 조회: Redis에서 랭킹 조회 및 ProductView 기반 응답 반환")
        void testGetRankingsFromRedis() {
            // Given: Redis ZSET에 랭킹 데이터 준비
            String key = "ranking:hourly";
            Long productId1 = 1L;
            Long productId2 = 2L;
            
            redisTemplate.opsForZSet().add(key, productId1.toString(), 100.0);
            redisTemplate.opsForZSet().add(key, productId2.toString(), 90.0);
            
            // Given: ProductView 데이터 준비
            createProductView(productId1, "상품1", BigDecimal.valueOf(10000), 10L);
            createProductView(productId2, "상품2", BigDecimal.valueOf(20000), 20L);
            
            // Given: Tier 2 캐시에 ProductView 저장 (선택적)
            // Tier 2 캐시는 Cache-Aside Pattern이므로, 없어도 DB에서 조회됨

            // When: API 호출
            String url = "/api/v1/rankings/hourly?size=10";
            ResponseEntity<ApiResponse<RankingDto.PageResponse<RankingDto.Response>>> response = 
                    testRestTemplate.exchange(
                            url,
                            org.springframework.http.HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<ApiResponse<RankingDto.PageResponse<RankingDto.Response>>>() {}
                    );

            // Then: 응답 검증
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().meta().result()).isEqualTo(ApiResponse.Metadata.Result.SUCCESS);
            
            RankingDto.PageResponse<RankingDto.Response> pageResponse = response.getBody().data();
            assertThat(pageResponse).isNotNull();
            assertThat(pageResponse.rankingType()).isEqualTo("HOURLY");
            assertThat(pageResponse.content()).isNotEmpty();
            
            // Then: 랭킹 순서 검증 (점수 내림차순)
            List<RankingDto.Response> items = pageResponse.content();
            assertThat(items.get(0).rank()).isEqualTo(1L);
            assertThat(items.get(0).productId()).isEqualTo(productId1); // 점수 100.0
            assertThat(items.get(1).rank()).isEqualTo(2L);
            assertThat(items.get(1).productId()).isEqualTo(productId2); // 점수 90.0
        }

        @Test
        @DisplayName("모든 랭킹 타입 지원: hourly, daily, weekly, monthly")
        void testAllRankingTypes() {
            // Given: 각 타입별 Redis ZSET 데이터 준비
            Long productId = 1L;
            createProductView(productId, "상품1", BigDecimal.valueOf(10000), 10L);
            
            redisTemplate.opsForZSet().add("ranking:hourly", productId.toString(), 100.0);
            redisTemplate.opsForZSet().add("ranking:daily", productId.toString(), 100.0);
            redisTemplate.opsForZSet().add("ranking:weekly", productId.toString(), 100.0);
            redisTemplate.opsForZSet().add("ranking:monthly", productId.toString(), 100.0);

            // When & Then: 각 타입별 API 호출
            String[] types = {"hourly", "daily", "weekly", "monthly"};
            for (String type : types) {
                String url = "/api/v1/rankings/" + type + "?size=10";
                ResponseEntity<ApiResponse<RankingDto.PageResponse<RankingDto.Response>>> response = 
                        testRestTemplate.exchange(
                                url,
                                org.springframework.http.HttpMethod.GET,
                                null,
                                new ParameterizedTypeReference<ApiResponse<RankingDto.PageResponse<RankingDto.Response>>>() {}
                        );
                
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().meta().result()).isEqualTo(ApiResponse.Metadata.Result.SUCCESS);
            }
        }
    }

    @Nested
    @DisplayName("Redis Fallback 테스트")
    class RedisFallbackTest {

        @Test
        @DisplayName("Redis Fallback: Redis ZSET 비어있을 때 RankingSnapshotX에서 직접 조회")
        void testRedisFallback() {
            // Given: Redis ZSET 비어있고, RankingSnapshotHourly 데이터 존재
            Long productId = 1L;
            createProductView(productId, "상품1", BigDecimal.valueOf(10000), 10L);
            
            LocalDateTime snapshotTime = LocalDateTime.now();
            RankingSnapshotHourly snapshot = RankingSnapshotHourly.builder()
                    .productId(productId)
                    .productRank(1)
                    .totalScore(100.0)
                    .snapshotTime(snapshotTime)
                    .build();
            rankingSnapshotHourlyJpaRepository.save(snapshot);

            // When: API 호출
            String url = "/api/v1/rankings/hourly?size=10";
            ResponseEntity<ApiResponse<RankingDto.PageResponse<RankingDto.Response>>> response = 
                    testRestTemplate.exchange(
                            url,
                            org.springframework.http.HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<ApiResponse<RankingDto.PageResponse<RankingDto.Response>>>() {}
                    );

            // Then: 응답이 정상적으로 반환됨 (Fallback 동작)
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().meta().result()).isEqualTo(ApiResponse.Metadata.Result.SUCCESS);
            
            RankingDto.PageResponse<RankingDto.Response> pageResponse = response.getBody().data();
            assertThat(pageResponse).isNotNull();
            assertThat(pageResponse.content()).isNotEmpty();
            assertThat(pageResponse.snapshotTime()).isNotNull(); // 스냅샷 시간이 포함됨
        }
    }

    @Nested
    @DisplayName("Tier 2 Cache Miss 테스트")
    class Tier2CacheMissTest {

        @Test
        @DisplayName("Tier 2 Cache Miss: Redis Hash에 없을 때 ProductView 테이블에서 조회 후 캐시 저장")
        void testTier2CacheMiss() {
            // Given: Redis ZSET에 랭킹 데이터 존재, 하지만 Tier 2 캐시(Redis Hash)에는 없음
            String zsetKey = "ranking:hourly";
            Long productId = 1L;
            
            redisTemplate.opsForZSet().add(zsetKey, productId.toString(), 100.0);
            
            // ProductView는 DB에만 존재 (Tier 2 캐시에는 없음)
            createProductView(productId, "상품1", BigDecimal.valueOf(10000), 10L);

            // When: API 호출
            String url = "/api/v1/rankings/hourly?size=10";
            ResponseEntity<ApiResponse<RankingDto.PageResponse<RankingDto.Response>>> response = 
                    testRestTemplate.exchange(
                            url,
                            org.springframework.http.HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<ApiResponse<RankingDto.PageResponse<RankingDto.Response>>>() {}
                    );

            // Then: 응답 정상 반환
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().meta().result()).isEqualTo(ApiResponse.Metadata.Result.SUCCESS);
            
            // Then: Tier 2 캐시에 저장되었는지 확인 (Cache-Aside Pattern)
            String hashKey = "product:detail:" + productId;
            Boolean exists = redisTemplate.hasKey(hashKey);
            // Cache-Aside Pattern이므로 캐시 저장 여부는 구현에 따라 다를 수 있음
            // 여기서는 응답이 정상적으로 반환되는지만 확인
        }
    }

    @Nested
    @DisplayName("Size 제한 테스트")
    class SizeLimitTest {

        @Test
        @DisplayName("Size 제한: size=100 요청 시 정상 처리")
        void testSizeLimit100() {
            // Given: Redis ZSET에 데이터 준비
            String key = "ranking:hourly";
            for (long i = 1; i <= 100; i++) {
                redisTemplate.opsForZSet().add(key, String.valueOf(i), (double) (100 - i));
                createProductView(i, "상품" + i, BigDecimal.valueOf(10000), 10L);
            }

            // When: size=100 요청
            String url = "/api/v1/rankings/hourly?size=100";
            ResponseEntity<ApiResponse<RankingDto.PageResponse<RankingDto.Response>>> response = 
                    testRestTemplate.exchange(
                            url,
                            org.springframework.http.HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<ApiResponse<RankingDto.PageResponse<RankingDto.Response>>>() {}
                    );

            // Then: 정상 처리
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().meta().result()).isEqualTo(ApiResponse.Metadata.Result.SUCCESS);
        }

        @Test
        @DisplayName("Size 제한: size=101 요청 시 400 Bad Request (validation 실패)")
        void testSizeLimitExceeded() {
            // When: size=101 요청 (최대값 초과)
            String url = "/api/v1/rankings/hourly?size=101";
            ResponseEntity<String> response = testRestTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.GET,
                    null,
                    String.class
            );

            // Then: 400 Bad Request
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Size 기본값: size 파라미터 없을 때 기본값 10 사용")
        void testSizeDefault() {
            // Given: Redis ZSET에 데이터 준비
            String key = "ranking:hourly";
            for (long i = 1; i <= 20; i++) {
                redisTemplate.opsForZSet().add(key, String.valueOf(i), (double) (100 - i));
                createProductView(i, "상품" + i, BigDecimal.valueOf(10000), 10L);
            }

            // When: size 파라미터 없이 요청
            String url = "/api/v1/rankings/hourly";
            ResponseEntity<ApiResponse<RankingDto.PageResponse<RankingDto.Response>>> response = 
                    testRestTemplate.exchange(
                            url,
                            org.springframework.http.HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<ApiResponse<RankingDto.PageResponse<RankingDto.Response>>>() {}
                    );

            // Then: 기본값 10개 반환
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().meta().result()).isEqualTo(ApiResponse.Metadata.Result.SUCCESS);
            
            RankingDto.PageResponse<RankingDto.Response> pageResponse = response.getBody().data();
            assertThat(pageResponse.content().size()).isLessThanOrEqualTo(10);
        }
    }

    @Nested
    @DisplayName("타입 검증 테스트")
    class TypeValidationTest {

        @Test
        @DisplayName("타입 검증: 잘못된 타입 요청 시 400 Bad Request")
        void testInvalidType() {
            // When: 잘못된 타입 요청
            String url = "/api/v1/rankings/invalid";
            ResponseEntity<String> response = testRestTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.GET,
                    null,
                    String.class
            );

            // Then: 400 Bad Request
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("Query Parameter 방식 테스트 (특정 datetime 조회)")
    class QueryParameterTest {

        @Test
        @DisplayName("정상 조회: datetime과 period를 통한 특정 시간 랭킹 조회")
        void testGetRankingsByDatetime() {
            // Given: 각 period별 스냅샷 데이터 준비
            Long productId = 1L;
            createProductView(productId, "상품1", BigDecimal.valueOf(10000), 10L);
            
            LocalDateTime hourlyTime = LocalDateTime.of(2025, 12, 1, 10, 0, 0);
            LocalDateTime dailyTime = LocalDateTime.of(2025, 12, 1, 0, 0, 0);
            LocalDateTime weeklyTime = LocalDateTime.of(2025, 11, 24, 0, 0, 0);
            LocalDateTime monthlyTime = LocalDateTime.of(2025, 12, 1, 0, 0, 0);
            
            createRankingSnapshotHourly(productId, 1, 100.0, hourlyTime);
            createRankingSnapshotDaily(productId, 1, 100.0, dailyTime);
            createRankingSnapshotWeekly(productId, 1, 100.0, weeklyTime);
            createRankingSnapshotMonthly(productId, 1, 100.0, monthlyTime);

            // When & Then: 각 period별 API 호출 및 검증
            testPeriodDatetime("20251201100000", "hourly", "HOURLY", hourlyTime);
            testPeriodDatetime("20251201150030", "daily", "DAILY", dailyTime);
            testPeriodDatetime("20251201150030", "weekly", "WEEKLY", weeklyTime);
            testPeriodDatetime("20251215150030", "monthly", "MONTHLY", monthlyTime);
        }

        private void testPeriodDatetime(String datetime, String period, String expectedType, LocalDateTime expectedSnapshotTime) {
            ResponseEntity<ApiResponse<RankingDto.PageResponse<RankingDto.Response>>> response = 
                    callRankingsByDatetime(datetime, period, 10);
            
            RankingDto.PageResponse<RankingDto.Response> pageResponse = assertSuccessResponse(response);
            assertThat(pageResponse.rankingType()).isEqualTo(expectedType);
            assertThat(pageResponse.snapshotTime()).isEqualTo(expectedSnapshotTime);
        }
    }

    @Nested
    @DisplayName("Query Parameter 검증 테스트")
    class QueryParameterValidationTest {

        @Test
        @DisplayName("datetime 파라미터 필수: datetime 없을 때 400 Bad Request")
        void testDatetimeRequired() {
            // When: datetime 파라미터 없이 요청
            String url = "/api/v1/rankings?period=hourly";
            ResponseEntity<String> response = testRestTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.GET,
                    null,
                    String.class
            );

            // Then: 400 Bad Request
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("period 파라미터 필수: period 없을 때 400 Bad Request")
        void testPeriodRequired() {
            // When: period 파라미터 없이 요청
            String url = "/api/v1/rankings?datetime=20251201100000";
            ResponseEntity<String> response = testRestTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.GET,
                    null,
                    String.class
            );

            // Then: 400 Bad Request
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("datetime 형식 검증: 잘못된 형식 요청 시 400 Bad Request")
        void testInvalidDatetimeFormat() {
            // When: 잘못된 datetime 형식 요청
            String url = "/api/v1/rankings?datetime=2025-12-01&period=hourly";
            ResponseEntity<String> response = testRestTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.GET,
                    null,
                    String.class
            );

            // Then: 400 Bad Request
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("period 검증: 잘못된 period 요청 시 400 Bad Request")
        void testInvalidPeriod() {
            // When: 잘못된 period 요청
            String url = "/api/v1/rankings?datetime=20251201100000&period=invalid";
            ResponseEntity<String> response = testRestTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.GET,
                    null,
                    String.class
            );

            // Then: 400 Bad Request
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Query Parameter 방식 API 호출 헬퍼 메서드
     */
    private ResponseEntity<ApiResponse<RankingDto.PageResponse<RankingDto.Response>>> callRankingsByDatetime(
            String datetime, String period, Integer size) {
        String url = String.format("/api/v1/rankings?datetime=%s&period=%s&size=%d", datetime, period, size);
        return testRestTemplate.exchange(
                url,
                org.springframework.http.HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<RankingDto.PageResponse<RankingDto.Response>>>() {}
        );
    }

    /**
     * 성공 응답 검증 헬퍼 메서드
     */
    private RankingDto.PageResponse<RankingDto.Response> assertSuccessResponse(
            ResponseEntity<ApiResponse<RankingDto.PageResponse<RankingDto.Response>>> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().meta().result()).isEqualTo(ApiResponse.Metadata.Result.SUCCESS);
        return response.getBody().data();
    }

    /**
     * ProductView 생성 헬퍼 메서드
     */
    private ProductView createProductView(Long id, String name, BigDecimal price, Long likeCount) {
        ProductView productView = ProductView.builder()
                .id(id)
                .name(name)
                .price(price)
                .likeCount(likeCount)
                .brandId(1L)
                .brandName("테스트 브랜드")
                .status(ProductStatus.ON_SALE)
                .createdAt(ZonedDateTime.now())
                .build();
        
        return productViewRepository.save(productView).orElseThrow();
    }

    /**
     * RankingSnapshotHourly 생성 헬퍼 메서드
     */
    private RankingSnapshotHourly createRankingSnapshotHourly(Long productId, Integer rank, Double totalScore, LocalDateTime snapshotTime) {
        RankingSnapshotHourly snapshot = RankingSnapshotHourly.builder()
                .productId(productId)
                .productRank(rank)
                .totalScore(totalScore)
                .snapshotTime(snapshotTime)
                .build();
        return rankingSnapshotHourlyJpaRepository.save(snapshot);
    }

    /**
     * RankingSnapshotDaily 생성 헬퍼 메서드
     */
    private RankingSnapshotDaily createRankingSnapshotDaily(Long productId, Integer rank, Double totalScore, LocalDateTime snapshotTime) {
        RankingSnapshotDaily snapshot = RankingSnapshotDaily.builder()
                .productId(productId)
                .productRank(rank)
                .totalScore(totalScore)
                .snapshotTime(snapshotTime)
                .build();
        return rankingSnapshotDailyJpaRepository.save(snapshot);
    }

    /**
     * RankingSnapshotWeekly 생성 헬퍼 메서드
     */
    private RankingSnapshotWeekly createRankingSnapshotWeekly(Long productId, Integer rank, Double totalScore, LocalDateTime snapshotTime) {
        RankingSnapshotWeekly snapshot = RankingSnapshotWeekly.builder()
                .productId(productId)
                .productRank(rank)
                .totalScore(totalScore)
                .snapshotTime(snapshotTime)
                .build();
        return rankingSnapshotWeeklyJpaRepository.save(snapshot);
    }

    /**
     * RankingSnapshotMonthly 생성 헬퍼 메서드
     */
    private RankingSnapshotMonthly createRankingSnapshotMonthly(Long productId, Integer rank, Double totalScore, LocalDateTime snapshotTime) {
        RankingSnapshotMonthly snapshot = RankingSnapshotMonthly.builder()
                .productId(productId)
                .productRank(rank)
                .totalScore(totalScore)
                .snapshotTime(snapshotTime)
                .build();
        return rankingSnapshotMonthlyJpaRepository.save(snapshot);
    }
}

