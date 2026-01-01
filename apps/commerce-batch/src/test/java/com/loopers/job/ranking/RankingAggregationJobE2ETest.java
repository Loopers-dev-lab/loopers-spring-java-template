package com.loopers.job.ranking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.loopers.batch.domain.metrics.ProductMetrics;
import com.loopers.batch.domain.ranking.WeeklyProductRank;
import com.loopers.batch.domain.ranking.MonthlyProductRank;
import com.loopers.infrastructure.ranking.WeeklyProductRankJpaRepository;
import com.loopers.infrastructure.ranking.MonthlyProductRankJpaRepository;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest
@SpringBatchTest
@TestPropertySource(properties = "spring.batch.job.name=rankingAggregationJob")
@Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Import(RankingJobTestConfig.class)
class RankingAggregationJobE2ETest {

  private static final AtomicLong RUN_ID_COUNTER = new AtomicLong(System.currentTimeMillis());
  private static final Long FIXED_UPDATED_AT = 1704067200000L; // 2024-01-01 00:00:00 UTC

  @Autowired
  private JobLauncherTestUtils jobLauncherTestUtils;

  @Autowired
  @Qualifier("rankingAggregationJob")
  private Job job;

  @Autowired
  private ProductMetricsTestRepository productMetricsRepository;

  @Autowired
  private WeeklyProductRankJpaRepository weeklyRepository;

  @Autowired
  private MonthlyProductRankJpaRepository monthlyRepository;

  @BeforeEach
  void setUp() {
    jobLauncherTestUtils.setJob(job);
  }

  @Nested
  @DisplayName("파라미터 검증")
  class ParameterValidation {

    @Test
    @DisplayName("period 파라미터가 없으면 Job이 실패한다")
    void shouldFail_whenPeriodMissing() throws Exception {
      // given
      var jobParameters = new JobParametersBuilder()
          .addLong("run.id", RUN_ID_COUNTER.incrementAndGet())
          .addString("baseDate", "20251224")
          .toJobParameters();

      // when
      var jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

      // then
      assertThat(jobExecution.getExitStatus().getExitCode())
          .isEqualTo(ExitStatus.FAILED.getExitCode());
    }

    @Test
    @DisplayName("baseDate 파라미터가 없으면 Job이 실패한다")
    void shouldFail_whenBaseDateMissing() throws Exception {
      // given
      var jobParameters = new JobParametersBuilder()
          .addLong("run.id", RUN_ID_COUNTER.incrementAndGet())
          .addString("period", "weekly")
          .toJobParameters();

      // when
      var jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

      // then
      assertThat(jobExecution.getExitStatus().getExitCode())
          .isEqualTo(ExitStatus.FAILED.getExitCode());
    }

    @Test
    @DisplayName("유효하지 않은 period 값이면 Job이 실패한다")
    void shouldFail_whenInvalidPeriod() throws Exception {
      // given
      var jobParameters = new JobParametersBuilder()
          .addLong("run.id", RUN_ID_COUNTER.incrementAndGet())
          .addString("period", "daily")
          .addString("baseDate", "20251224")
          .toJobParameters();

      // when
      var jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

      // then
      assertThat(jobExecution.getExitStatus().getExitCode())
          .isEqualTo(ExitStatus.FAILED.getExitCode());
    }
  }

  @Nested
  @DisplayName("주간 랭킹 집계")
  class WeeklyAggregation {

    @Test
    @DisplayName("주간 메트릭을 집계하여 MV 테이블에 저장한다")
    void shouldAggregateWeeklyMetrics() throws Exception {
      // given: 2025-12-22 (월) ~ 2025-12-28 (일) 주간 데이터
      saveAllMetrics(
          metrics(1L, 20251222, 100L, 10L, 5L),  // 월
          metrics(1L, 20251223, 50L, 5L, 2L),    // 화
          metrics(2L, 20251222, 200L, 20L, 10L)  // 상품2가 더 높은 점수
      );

      var jobParameters = new JobParametersBuilder()
          .addLong("run.id", RUN_ID_COUNTER.incrementAndGet())
          .addString("period", "weekly")
          .addString("baseDate", "20251224")  // 수요일 기준
          .toJobParameters();

      // when
      var jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

      // then
      assertAll(
          () -> assertThat(jobExecution.getExitStatus().getExitCode())
              .isEqualTo(ExitStatus.COMPLETED.getExitCode()),
          () -> {
            List<WeeklyProductRank> ranks = weeklyRepository.findAll();
            // 상품1: (100+50)*0.1 + (10+5)*0.3 + (5+2)*0.6 = 23.7
            // 상품2: 200*0.1 + 20*0.3 + 10*0.6 = 32.0
            assertThat(ranks)
                .hasSize(2)
                .extracting(WeeklyProductRank::getScore)
                .containsExactlyInAnyOrder(23.7, 32.0);
          }
      );
    }

    @Test
    @DisplayName("연도 경계: 2024-12-30은 2025-W01에 속한다")
    void shouldHandleYearBoundary_weekBelongsToNextYear() throws Exception {
      // given: 2024-12-30 (월) ~ 2025-01-05 (일) = 2025-W01
      saveAllMetrics(
          metrics(1L, 20241230, 100L, 10L, 5L),
          metrics(1L, 20250101, 50L, 5L, 2L)
      );

      var jobParameters = new JobParametersBuilder()
          .addLong("run.id", RUN_ID_COUNTER.incrementAndGet())
          .addString("period", "weekly")
          .addString("baseDate", "20241231")  // 2024년 마지막 날이지만 2025-W01
          .toJobParameters();

      // when
      var jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

      // then
      assertAll(
          () -> assertThat(jobExecution.getExitStatus().getExitCode())
              .isEqualTo(ExitStatus.COMPLETED.getExitCode()),
          () -> {
            List<WeeklyProductRank> ranks = weeklyRepository.findAll();
            assertThat(ranks).isNotEmpty();
            assertThat(ranks.get(0).getYearWeek()).isEqualTo("2025-W01");
          }
      );
    }
  }

  @Nested
  @DisplayName("월간 랭킹 집계")
  class MonthlyAggregation {

    @Test
    @DisplayName("월간 메트릭을 집계하여 MV 테이블에 저장한다")
    void shouldAggregateMonthlyMetrics() throws Exception {
      // given: 2025년 12월 전체 데이터
      saveAllMetrics(
          metrics(1L, 20251201, 100L, 10L, 5L),
          metrics(1L, 20251215, 100L, 10L, 5L),
          metrics(1L, 20251231, 100L, 10L, 5L)
      );

      var jobParameters = new JobParametersBuilder()
          .addLong("run.id", RUN_ID_COUNTER.incrementAndGet())
          .addString("period", "monthly")
          .addString("baseDate", "20251215")
          .toJobParameters();

      // when
      var jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

      // then
      assertAll(
          () -> assertThat(jobExecution.getExitStatus().getExitCode())
              .isEqualTo(ExitStatus.COMPLETED.getExitCode()),
          () -> {
            List<MonthlyProductRank> ranks = monthlyRepository.findAll();
            assertThat(ranks).hasSize(1);
            assertThat(ranks.get(0).getYearMonth()).isEqualTo("2025-12");
          }
      );
    }
  }

  @Nested
  @DisplayName("점수 계산 검증")
  class ScoreCalculation {

    @Test
    @DisplayName("메트릭 가중치에 따라 점수가 계산된다")
    void shouldCalculateScore_withConfiguredWeights() throws Exception {
      // given: score = view*viewWeight + like*likeWeight + sales*orderWeight
      saveAllMetrics(metrics(1L, 20251222, 100L, 10L, 5L));

      var jobParameters = new JobParametersBuilder()
          .addLong("run.id", RUN_ID_COUNTER.incrementAndGet())
          .addString("period", "weekly")
          .addString("baseDate", "20251224")
          .toJobParameters();

      // when
      var jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

      // then
      List<WeeklyProductRank> ranks = weeklyRepository.findAll();
      assertThat(ranks).hasSize(1);
      assertThat(ranks.get(0).getScore()).isEqualTo(16.0);
    }
  }

  private void saveAllMetrics(ProductMetrics... metrics) {
    productMetricsRepository.saveAll(List.of(metrics));
  }

  private ProductMetrics metrics(
      Long productId, Integer metricDate, Long viewCount, Long likeCount, Long salesCount) {
    return ProductMetrics.of(
        productId, metricDate, viewCount, likeCount, salesCount, FIXED_UPDATED_AT);
  }
}
