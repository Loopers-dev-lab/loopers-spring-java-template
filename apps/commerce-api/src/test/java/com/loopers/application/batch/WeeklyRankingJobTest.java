package com.loopers.application.batch;

import com.loopers.domain.ranking.weekly.WeeklyRanking;
import com.loopers.domain.ranking.weekly.WeeklyRankingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WeeklyRankingJob 통합 테스트
 * - TestContainers로 MySQL 실행
 * - 샘플 ProductMetrics 데이터 생성
 * - 배치 실행 및 결과 검증
 */
@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = "/db/init-ranking-tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class WeeklyRankingJobTest {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job weeklyRankingJob;

    @Autowired
    private WeeklyRankingRepository weeklyRankingRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // 기존 데이터 정리
        jdbcTemplate.execute("DELETE FROM mv_product_rank_weekly");
        jdbcTemplate.execute("DELETE FROM product_metrics");
    }

    @Test
    @DisplayName("주간 랭킹 배치가 ProductMetrics를 읽어서 TOP 100 랭킹을 생성한다")
    void shouldGenerateWeeklyRankingFromProductMetrics() throws Exception {
        // Given: 샘플 ProductMetrics 데이터 생성 (150개)
        insertSampleProductMetrics(150);

        LocalDate targetDate = LocalDate.of(2024, 12, 30); // 월요일
        LocalDate weekStartDate = LocalDate.of(2024, 12, 30); // 월요일
        LocalDate weekEndDate = LocalDate.of(2025, 1, 5);   // 일요일

        // When: 주간 랭킹 배치 실행
        JobParameters jobParameters = new JobParametersBuilder()
            .addString("targetDate", targetDate.toString())
            .addLong("timestamp", System.currentTimeMillis()) // 유니크 파라미터
            .toJobParameters();

        JobExecution jobExecution = jobLauncher.run(weeklyRankingJob, jobParameters);

        // Then: 배치 실행 성공
        if (!jobExecution.getExitStatus().getExitCode().equals("COMPLETED")) {
            System.out.println("=== Job Execution Failed ===");
            System.out.println("Exit Status: " + jobExecution.getExitStatus());
            System.out.println("Exit Description: " + jobExecution.getExitStatus().getExitDescription());
            jobExecution.getStepExecutions().forEach(step -> {
                System.out.println("Step: " + step.getStepName());
                System.out.println("  Exit Status: " + step.getExitStatus());
                System.out.println("  Failures: " + step.getFailureExceptions());
                step.getFailureExceptions().forEach(Throwable::printStackTrace);
            });
        }
        assertThat(jobExecution.getExitStatus().getExitCode()).isEqualTo("COMPLETED");

        // Then: TOP 100만 저장되었는지 확인
        List<WeeklyRanking> rankings = weeklyRankingRepository
            .findByWeekStartDateOrderByRankPosition(weekStartDate);

        assertThat(rankings).hasSize(100);

        // Then: 순위가 올바르게 설정되었는지 확인 (1위부터 100위까지)
        for (int i = 0; i < 100; i++) {
            WeeklyRanking ranking = rankings.get(i);
            assertThat(ranking.getRankPosition()).isEqualTo(i + 1);
            assertThat(ranking.getWeekStartDate()).isEqualTo(weekStartDate);
            assertThat(ranking.getWeekEndDate()).isEqualTo(weekEndDate);
        }

        // Then: 점수가 내림차순으로 정렬되었는지 확인
        for (int i = 0; i < rankings.size() - 1; i++) {
            assertThat(rankings.get(i).getTotalScore())
                .isGreaterThanOrEqualTo(rankings.get(i + 1).getTotalScore());
        }

        // Then: 1위 상품이 가장 높은 점수를 가지는지 확인
        WeeklyRanking firstRank = rankings.get(0);
        assertThat(firstRank.getRankPosition()).isEqualTo(1);
        assertThat(firstRank.getTotalScore()).isGreaterThan(0);
    }

    @Test
    @DisplayName("ProductMetrics가 100개 미만일 때 모든 데이터를 랭킹에 포함한다")
    void shouldIncludeAllDataWhenLessThan100() throws Exception {
        // Given: 50개의 ProductMetrics 데이터
        insertSampleProductMetrics(50);

        LocalDate targetDate = LocalDate.of(2024, 12, 30);
        LocalDate weekStartDate = LocalDate.of(2024, 12, 30);

        // When: 배치 실행
        JobParameters jobParameters = new JobParametersBuilder()
            .addString("targetDate", targetDate.toString())
            .addLong("timestamp", System.currentTimeMillis())
            .toJobParameters();

        JobExecution jobExecution = jobLauncher.run(weeklyRankingJob, jobParameters);

        // Then: 50개 모두 저장
        assertThat(jobExecution.getExitStatus().getExitCode()).isEqualTo("COMPLETED");

        List<WeeklyRanking> rankings = weeklyRankingRepository
            .findByWeekStartDateOrderByRankPosition(weekStartDate);

        assertThat(rankings).hasSize(50);
    }

    @Test
    @DisplayName("기존 주간 랭킹 데이터가 있으면 삭제 후 새로 생성한다")
    void shouldDeleteOldRankingBeforeCreatingNew() throws Exception {
        // Given: 기존 주간 랭킹 데이터 생성
        LocalDate targetDate = LocalDate.of(2024, 12, 30);
        LocalDate weekStartDate = LocalDate.of(2024, 12, 30);
        LocalDate weekEndDate = LocalDate.of(2025, 1, 5);

        WeeklyRanking oldRanking = new WeeklyRanking(
            1, 999L, 100.0, weekStartDate, weekEndDate
        );
        weeklyRankingRepository.saveAll(List.of(oldRanking));

        // Given: 새로운 ProductMetrics 데이터
        insertSampleProductMetrics(10);

        // When: 배치 실행
        JobParameters jobParameters = new JobParametersBuilder()
            .addString("targetDate", targetDate.toString())
            .addLong("timestamp", System.currentTimeMillis())
            .toJobParameters();

        jobLauncher.run(weeklyRankingJob, jobParameters);

        // Then: 기존 데이터는 삭제되고 새 데이터만 존재
        List<WeeklyRanking> rankings = weeklyRankingRepository
            .findByWeekStartDateOrderByRankPosition(weekStartDate);

        assertThat(rankings).hasSize(10);
        assertThat(rankings).noneMatch(r -> r.getProductId().equals(999L));
    }

    /**
     * 샘플 ProductMetrics 데이터 생성
     * - productId: 1부터 count까지
     * - 점수가 다양하도록 랜덤하게 생성
     */
    private void insertSampleProductMetrics(int count) {
        for (long i = 1; i <= count; i++) {
            long likeCount = (count - i + 1) * 10;    // 역순으로 점수 부여
            long viewCount = (count - i + 1) * 100;
            long salesCount = (count - i + 1) * 5;
            long salesAmount = (count - i + 1) * 50000;

            jdbcTemplate.update(
                "INSERT INTO product_metrics (product_id, like_count, view_count, sales_count, sales_amount, last_updated) " +
                "VALUES (?, ?, ?, ?, ?, NOW())",
                i, likeCount, viewCount, salesCount, salesAmount
            );
        }
    }
}
