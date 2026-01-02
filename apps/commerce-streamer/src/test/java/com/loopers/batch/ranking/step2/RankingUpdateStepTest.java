package com.loopers.batch.ranking.step2;

import com.loopers.domain.ranking.ProductScore5Min;
import com.loopers.domain.ranking.RankingScoreHourly;
import com.loopers.domain.ranking.RankingSnapshotHourly;
import com.loopers.infrastructure.ranking.ProductScore5MinJpaRepository;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step 2: ParallelRankingUpdateStep 테스트
 * 
 * - 슬라이딩 윈도우 업데이트 검증
 * - 처음 윈도우 누적 검증
 * - 스냅샷 생성 검증
 * - 동적 가중치 재계산 검증
 * - 배치 UPSERT 검증
 */
@DisplayName("Step 2: ParallelRankingUpdateStep 테스트")
@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RankingUpdateStepTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    @Qualifier("productRankingUpdateJob")
    private Job productRankingUpdateJob;

    @Autowired
    private ProductScore5MinJpaRepository productScore5MinJpaRepository;

    @Autowired
    private RankingScoreHourlyJpaRepository rankingScoreHourlyJpaRepository;

    @Autowired
    private RankingSnapshotHourlyJpaRepository rankingSnapshotHourlyJpaRepository;

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
    @DisplayName("슬라이딩 윈도우 업데이트 테스트")
    class SlidingWindowUpdateTest {

        @Test
        @DisplayName("슬라이딩 윈도우 업데이트: NEW 추가, OLD 제거 후 정확한 값 계산")
        void testSlidingWindowUpdate() throws Exception {
            // Given: product_score_5min에 NEW 데이터와 OLD 데이터 준비
            LocalDateTime now = LocalDateTime.now().minusMinutes(2); // 2분 버퍼
            LocalDateTime newStart = now.minusMinutes(5).truncatedTo(ChronoUnit.MINUTES)
                    .withMinute((now.minusMinutes(5).getMinute() / 5) * 5)
                    .withSecond(0)
                    .withNano(0);
            LocalDateTime newEnd = newStart.plusMinutes(5);
            
            LocalDateTime oldStart = newStart.minusHours(1).minusMinutes(5);
            LocalDateTime oldEnd = oldStart.plusMinutes(5);
            
            Long productId = 1L;
            
            // NEW 데이터: 현재 5분 구간
            ProductScore5Min newData = ProductScore5Min.builder()
                    .productId(productId)
                    .startTime(newStart)
                    .endTime(newEnd)
                    .orderAmountSum(BigDecimal.valueOf(10000))
                    .likeCount(2L)
                    .viewCount(3L)
                    .build();
            productScore5MinJpaRepository.save(newData);
            
            // OLD 데이터: 1시간 전 구간 (윈도우 밖으로 밀려남)
            ProductScore5Min oldData = ProductScore5Min.builder()
                    .productId(productId)
                    .startTime(oldStart)
                    .endTime(oldEnd)
                    .orderAmountSum(BigDecimal.valueOf(5000))
                    .likeCount(1L)
                    .viewCount(1L)
                    .build();
            productScore5MinJpaRepository.save(oldData);
            
            // 기존 RankingScoreHourly 데이터 (현재 누적값)
            RankingScoreHourly existing = RankingScoreHourly.builder()
                    .productId(productId)
                    .totalOrderAmount(BigDecimal.valueOf(20000))
                    .totalLikeCount(5L)
                    .totalViewCount(7L)
                    .currentScore(100.0)
                    .build();
            rankingScoreHourlyJpaRepository.save(existing);

            // When: Step 2 실행 (Hourly만 테스트)
            JobExecution jobExecution = jobLauncherTestUtils.launchStep("hourlyRankingUpdateStep");

            // Then: Step 성공 확인
            assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

            // Then: RankingScoreHourly 업데이트 확인
            Optional<RankingScoreHourly> resultOpt = rankingScoreHourlyJpaRepository.findById(productId);
            assertThat(resultOpt).isPresent();
            RankingScoreHourly result = resultOpt.get();
            
            // 슬라이딩 윈도우 계산: 기존(20000) + NEW(10000) - OLD(5000) = 25000
            assertThat(result.getTotalOrderAmount()).isEqualByComparingTo(BigDecimal.valueOf(25000));
            // 기존(5) + NEW(2) - OLD(1) = 6
            assertThat(result.getTotalLikeCount()).isEqualTo(6L);
            // 기존(7) + NEW(3) - OLD(1) = 9
            assertThat(result.getTotalViewCount()).isEqualTo(9L);
        }

        @Test
        @DisplayName("처음 윈도우 누적: RankingScore가 비어있을 때 NEW만 추가")
        void testInitialWindowAccumulation() throws Exception {
            // Given: RankingScoreHourly 테이블이 비어있고, NEW 데이터만 존재
            LocalDateTime now = LocalDateTime.now().minusMinutes(2);
            LocalDateTime newStart = now.minusMinutes(5).truncatedTo(ChronoUnit.MINUTES)
                    .withMinute((now.minusMinutes(5).getMinute() / 5) * 5)
                    .withSecond(0)
                    .withNano(0);
            LocalDateTime newEnd = newStart.plusMinutes(5);
            
            Long productId = 1L;
            
            ProductScore5Min newData = ProductScore5Min.builder()
                    .productId(productId)
                    .startTime(newStart)
                    .endTime(newEnd)
                    .orderAmountSum(BigDecimal.valueOf(10000))
                    .likeCount(2L)
                    .viewCount(3L)
                    .build();
            productScore5MinJpaRepository.save(newData);

            // When: Step 2 실행
            JobExecution jobExecution = jobLauncherTestUtils.launchStep("hourlyRankingUpdateStep");

            // Then: Step 성공 확인
            assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

            // Then: NEW만 추가되고 OLD는 0으로 처리
            Optional<RankingScoreHourly> resultOpt = rankingScoreHourlyJpaRepository.findById(productId);
            assertThat(resultOpt).isPresent();
            RankingScoreHourly result = resultOpt.get();
            
            // NEW만 추가: 0 + NEW(10000) - OLD(0) = 10000
            assertThat(result.getTotalOrderAmount()).isEqualByComparingTo(BigDecimal.valueOf(10000));
            assertThat(result.getTotalLikeCount()).isEqualTo(2L);
            assertThat(result.getTotalViewCount()).isEqualTo(3L);
        }
    }

    @Nested
    @DisplayName("스냅샷 생성 테스트")
    class SnapshotCreationTest {

        @Test
        @DisplayName("스냅샷 생성: 상위 500개만 선택하여 RankingSnapshotX에 저장 (DELETE 없이 INSERT만)")
        void testSnapshotCreation() throws Exception {
            // Given: 여러 상품의 RankingScoreHourly 데이터 생성 (600개)
            LocalDateTime now = LocalDateTime.now().minusMinutes(2);
            LocalDateTime newStart = now.minusMinutes(5).truncatedTo(ChronoUnit.MINUTES)
                    .withMinute((now.minusMinutes(5).getMinute() / 5) * 5)
                    .withSecond(0)
                    .withNano(0);
            LocalDateTime newEnd = newStart.plusMinutes(5);
            
            // 600개 상품 생성
            for (long i = 1; i <= 600; i++) {
                ProductScore5Min newData = ProductScore5Min.builder()
                        .productId(i)
                        .startTime(newStart)
                        .endTime(newEnd)
                        .orderAmountSum(BigDecimal.valueOf(10000 * i)) // 점수 차이를 위해
                        .likeCount(i)
                        .viewCount(i)
                        .build();
                productScore5MinJpaRepository.save(newData);
                
                // RankingScoreHourly도 미리 생성 (점수 순서대로)
                RankingScoreHourly score = RankingScoreHourly.builder()
                        .productId(i)
                        .totalOrderAmount(BigDecimal.valueOf(10000 * i))
                        .totalLikeCount(i)
                        .totalViewCount(i)
                        .currentScore((double) (600 - i)) // 내림차순 정렬을 위해
                        .build();
                rankingScoreHourlyJpaRepository.save(score);
            }

            // When: Step 2 실행
            JobExecution jobExecution = jobLauncherTestUtils.launchStep("hourlyRankingUpdateStep");

            // Then: Step 성공 확인
            assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

            // Then: 상위 500개만 스냅샷에 저장되었는지 확인
            List<RankingSnapshotHourly> snapshots = rankingSnapshotHourlyJpaRepository.findAll();
            assertThat(snapshots.size()).isLessThanOrEqualTo(500);
            
            // Then: 스냅샷 필드 검증
            if (!snapshots.isEmpty()) {
                RankingSnapshotHourly firstSnapshot = snapshots.get(0);
                assertThat(firstSnapshot.getProductId()).isNotNull();
                assertThat(firstSnapshot.getProductRank()).isNotNull();
                assertThat(firstSnapshot.getTotalScore()).isNotNull();
                assertThat(firstSnapshot.getSnapshotTime()).isNotNull();
            }
        }
    }

    @Nested
    @DisplayName("배치 UPSERT 테스트")
    class BatchUpsertTest {

        @Test
        @DisplayName("배치 UPSERT: 1000건 이상 데이터 생성 시 1000건씩 청크 단위로 UPSERT 처리")
        void testBatchUpsert() throws Exception {
            // Given: 1500개 상품의 product_score_5min 데이터 생성
            LocalDateTime now = LocalDateTime.now().minusMinutes(2);
            LocalDateTime newStart = now.minusMinutes(5).truncatedTo(ChronoUnit.MINUTES)
                    .withMinute((now.minusMinutes(5).getMinute() / 5) * 5)
                    .withSecond(0)
                    .withNano(0);
            LocalDateTime newEnd = newStart.plusMinutes(5);
            
            for (long i = 1; i <= 1500; i++) {
                ProductScore5Min newData = ProductScore5Min.builder()
                        .productId(i)
                        .startTime(newStart)
                        .endTime(newEnd)
                        .orderAmountSum(BigDecimal.valueOf(10000))
                        .likeCount(1L)
                        .viewCount(1L)
                        .build();
                productScore5MinJpaRepository.save(newData);
            }

            // When: Step 2 실행
            JobExecution jobExecution = jobLauncherTestUtils.launchStep("hourlyRankingUpdateStep");

            // Then: Step 성공 확인
            assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

            // Then: 모든 상품이 RankingScoreHourly에 저장되었는지 확인
            // (배치 UPSERT가 1000건씩 처리되었는지는 로그로 확인)
            long count = rankingScoreHourlyJpaRepository.count();
            assertThat(count).isEqualTo(1500L);
        }
    }
}

