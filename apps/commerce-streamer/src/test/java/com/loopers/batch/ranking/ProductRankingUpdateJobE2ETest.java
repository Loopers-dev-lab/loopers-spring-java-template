package com.loopers.batch.ranking;

import com.loopers.domain.ranking.ProductScore5Min;
import com.loopers.domain.ranking.RankingEventLog;
import com.loopers.domain.ranking.RankingEventType;
import com.loopers.domain.ranking.RankingScoreHourly;
import com.loopers.domain.ranking.RankingSnapshotHourly;
import com.loopers.infrastructure.ranking.ProductScore5MinJpaRepository;
import com.loopers.infrastructure.ranking.RankingEventLogJpaRepository;
import com.loopers.infrastructure.ranking.RankingScoreHourlyJpaRepository;
import com.loopers.infrastructure.ranking.RankingSnapshotHourlyJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import com.loopers.utils.RedisCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ProductRankingUpdateJob E2E 테스트
 * 
 * 전체 플로우 검증:
 * RankingEventLog → Step 1 → product_score_5min → Step 2 → RankingScoreX → RankingSnapshotX → Step 3-1 → Redis ZSET → Step 3-2 → Redis Hash
 */
@DisplayName("ProductRankingUpdateJob E2E 테스트")
@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductRankingUpdateJobE2ETest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    @Qualifier("productRankingUpdateJob")
    private Job productRankingUpdateJob;

    @Autowired
    private RankingEventLogJpaRepository rankingEventLogJpaRepository;

    @Autowired
    private ProductScore5MinJpaRepository productScore5MinJpaRepository;

    @Autowired
    private RankingScoreHourlyJpaRepository rankingScoreHourlyJpaRepository;

    @Autowired
    private RankingSnapshotHourlyJpaRepository rankingSnapshotHourlyJpaRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private RedisCleanUp redisCleanUp;

    @BeforeEach
    void setUp() {
        jobLauncherTestUtils.setJob(productRankingUpdateJob);
    }

    @AfterEach
    void tearDown() {
        redisCleanUp.truncateAll();
        databaseCleanUp.truncateAllTables();
    }

    @Nested
    @DisplayName("정상 실행 플로우 테스트")
    class FullJobExecutionTest {

        @Test
        @DisplayName("정상 실행 플로우: 전체 Job 실행 및 각 단계별 데이터 검증")
        void testFullJobExecution() throws Exception {
            // Given: RankingEventLog 데이터 생성
            LocalDateTime baseTime = LocalDateTime.now().minusMinutes(10).truncatedTo(ChronoUnit.MINUTES)
                    .withMinute((LocalDateTime.now().getMinute() / 5) * 5)
                    .withSecond(0)
                    .withNano(0);
            
            Long productId = 1L;
            createRankingEventLog(productId, RankingEventType.ORDER, baseTime.plusMinutes(1), 
                    BigDecimal.valueOf(10000), 2);
            createRankingEventLog(productId, RankingEventType.LIKE, baseTime.plusMinutes(2));
            createRankingEventLog(productId, RankingEventType.VIEW, baseTime.plusMinutes(3));

            // When: Job 실행
            JobExecution jobExecution = jobLauncherTestUtils.launchJob();

            // Then: Job 성공 확인
            assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

            // Then: Step 1 검증 - product_score_5min 데이터 확인
            LocalDateTime startTime = baseTime;
            LocalDateTime endTime = baseTime.plusMinutes(5);
            Optional<ProductScore5Min> step1Result = productScore5MinJpaRepository
                    .findByProductIdAndTimeRange(productId, startTime, endTime);
            assertThat(step1Result).isPresent();
            assertThat(step1Result.get().getOrderAmountSum()).isEqualByComparingTo(BigDecimal.valueOf(20000));

            // Then: Step 2 검증 - RankingScoreHourly 데이터 확인
            Optional<RankingScoreHourly> step2Result = rankingScoreHourlyJpaRepository.findById(productId);
            assertThat(step2Result).isPresent();
            assertThat(step2Result.get().getTotalOrderAmount()).isGreaterThan(BigDecimal.ZERO);

            // Then: Step 2 검증 - RankingSnapshotHourly 데이터 확인
            List<RankingSnapshotHourly> snapshots = rankingSnapshotHourlyJpaRepository.findAll();
            assertThat(snapshots).isNotEmpty();

            // Then: Step 3-1 검증 - Redis ZSET 데이터 확인
            Set<String> zsetMembers = redisTemplate.opsForZSet().reverseRange("ranking:hourly", 0, -1);
            assertThat(zsetMembers).isNotEmpty();
            assertThat(zsetMembers).contains(productId.toString());

            // Then: Step 3-2 검증 - Redis Hash 데이터 확인 (선택적)
            // Tier 2 캐시는 ProductView 데이터가 필요하므로, 여기서는 기본 검증만 수행
        }
    }

    @Nested
    @DisplayName("처음 실행 시나리오 테스트")
    class InitialExecutionTest {

        @Test
        @DisplayName("처음 실행 시나리오: RankingScore 테이블이 비어있을 때 데이터가 천천히 쌓이는지 확인")
        void testInitialExecution() throws Exception {
            // Given: RankingScoreHourly 테이블이 비어있고, RankingEventLog 데이터만 존재
            LocalDateTime baseTime = LocalDateTime.now().minusMinutes(10).truncatedTo(ChronoUnit.MINUTES)
                    .withMinute((LocalDateTime.now().getMinute() / 5) * 5)
                    .withSecond(0)
                    .withNano(0);
            
            Long productId = 1L;
            createRankingEventLog(productId, RankingEventType.ORDER, baseTime.plusMinutes(1), 
                    BigDecimal.valueOf(10000), 1);

            // When: Job 실행
            JobExecution jobExecution = jobLauncherTestUtils.launchJob();

            // Then: Job 성공 확인
            assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

            // Then: NEW만 추가되고 OLD는 0으로 처리되었는지 확인
            Optional<RankingScoreHourly> result = rankingScoreHourlyJpaRepository.findById(productId);
            assertThat(result).isPresent();
            RankingScoreHourly score = result.get();
            
            // 처음 실행이므로 NEW만 추가: 0 + NEW(10000) - OLD(0) = 10000
            assertThat(score.getTotalOrderAmount()).isEqualByComparingTo(BigDecimal.valueOf(10000));
        }
    }

    @Nested
    @DisplayName("Catch-up 로직 테스트")
    class CatchUpTest {

        @Test
        @DisplayName("중간 장애 후 복구: 이전 Job 실패로 일부 구간 누락 시 Catch-up 로직으로 자동 처리 확인")
        void testCatchUpAfterFailure() throws Exception {
            // Given: 기존 product_score_5min 데이터 생성 (과거 구간)
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime pastTime = now.minusMinutes(20).truncatedTo(ChronoUnit.MINUTES)
                    .withMinute(((now.getMinute() - 20) / 5) * 5)
                    .withSecond(0)
                    .withNano(0);
            
            Long productId = 1L;
            
            // 기존 데이터: 20분 전 구간
            ProductScore5Min existing = ProductScore5Min.builder()
                    .productId(productId)
                    .startTime(pastTime)
                    .endTime(pastTime.plusMinutes(5))
                    .orderAmountSum(BigDecimal.valueOf(5000))
                    .likeCount(0L)
                    .viewCount(0L)
                    .build();
            productScore5MinJpaRepository.save(existing);
            
            // 누락된 구간의 이벤트 생성 (15분 전, 10분 전)
            LocalDateTime missingTime1 = pastTime.plusMinutes(5);
            LocalDateTime missingTime2 = pastTime.plusMinutes(10);
            LocalDateTime currentTime = now.minusMinutes(3); // 2분 버퍼 내
            
            createRankingEventLog(productId, RankingEventType.ORDER, missingTime1.plusMinutes(1), 
                    BigDecimal.valueOf(10000), 1);
            createRankingEventLog(productId, RankingEventType.ORDER, missingTime2.plusMinutes(1), 
                    BigDecimal.valueOf(20000), 1);
            createRankingEventLog(productId, RankingEventType.ORDER, currentTime, 
                    BigDecimal.valueOf(30000), 1);

            // When: Job 실행
            JobExecution jobExecution = jobLauncherTestUtils.launchJob();

            // Then: Job 성공 확인
            assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

            // Then: 누락된 구간들이 모두 처리되었는지 확인
            // MAX(end_time) = pastTime + 5분이므로, 그 이후 구간들이 처리되어야 함
            List<ProductScore5Min> allResults = productScore5MinJpaRepository.findAll();
            assertThat(allResults.size()).isGreaterThanOrEqualTo(3); // 기존 1개 + 누락 구간 2개 이상
        }
    }

    /**
     * RankingEventLog 생성 헬퍼 메서드
     */
    private RankingEventLog createRankingEventLog(Long productId, RankingEventType eventType, 
                                                  LocalDateTime occurredAt, BigDecimal price, Integer quantity) {
        String eventId = UUID.randomUUID().toString();
        Double score = 0.0; // 테스트에서는 점수는 중요하지 않음
        
        RankingEventLog eventLog = RankingEventLog.builder()
                .eventId(eventId)
                .productId(productId)
                .eventType(eventType)
                .score(score)
                .occurredAt(occurredAt)
                .rawPrice(price)
                .rawQuantity(quantity)
                .build();
        
        return rankingEventLogJpaRepository.save(eventLog);
    }

    private RankingEventLog createRankingEventLog(Long productId, RankingEventType eventType, LocalDateTime occurredAt) {
        return createRankingEventLog(productId, eventType, occurredAt, null, null);
    }
}

